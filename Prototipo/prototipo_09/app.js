(() => {
  const $ = (s) => document.querySelector(s);
  const $$ = (s) => [...document.querySelectorAll(s)];

  const monthNames = ["Janeiro","Fevereiro","Março","Abril","Maio","Junho","Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"];
  const shortMonthNames = ["Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"];
  const categories = ["Alimentação","Moradia","Transporte","Lazer","Saúde","Outros"];

  // Período "atual" do protótipo.
  // Mantemos fixo para o teste ficar previsível e coerente com os dados de setembro de 2026.
  const PROTOTYPE_CURRENT_PERIOD = "2026-09";

  const state = {
    selectedPeriod: "2026-09",
    pickerYear: 2026,
    incomes: [],
    expenses: [],
    recurrences: [],
    exceptions: [],
    editing: null,
    detailRef: null,
    listContext: { type: "expense", category: null },
    returnScreen: "screen-dashboard",
    pendingScopeSave: null,
    pendingDelete: null,
    onboardingExpenseMode: false,
    viewingHistoricalPeriod: false,
    historyYearChartOpen: false,
    scrollPositions: {}
  };

  const uid = () => (globalThis.crypto?.randomUUID?.() || `id-${Date.now()}-${Math.random().toString(16).slice(2)}`);
  const money = (n) => new Intl.NumberFormat("pt-BR",{style:"currency",currency:"BRL"}).format(n || 0);
  const periodLabel = (p) => {
    const [y,m] = p.split("-").map(Number);
    return `${monthNames[m-1]} de ${y}`;
  };
  const periodFromDate = (d) => d.slice(0,7);
  const todayInPeriod = (p, day=4) => `${p}-${String(Math.min(day,28)).padStart(2,"0")}`;
  const comparePeriod = (a,b) => a.localeCompare(b);
  const prevPeriod = (p) => shiftPeriod(p,-1);
  const nextPeriod = (p) => shiftPeriod(p,1);
  const shiftPeriod = (p,delta) => {
    const [y,m] = p.split("-").map(Number);
    const d = new Date(y,m-1+delta,1);
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}`;
  };

  function currentScreenId(){
    const active=document.querySelector(".screen.active");
    return active ? active.id : null;
  }

  function saveCurrentScroll(){
    const id=currentScreenId();
    if(id){
      state.scrollPositions[id]=window.scrollY;
    }
  }

  function resetDashboardChart(){
    const card=$("#chart-card");
    const button=$("#toggle-chart-btn");

    if(card){
      card.classList.add("hidden");
    }

    if(button){
      button.textContent="Ver gráfico das despesas";
      button.setAttribute("aria-expanded","false");
    }
  }

  function showScreen(id, restoreScroll=false){
    const currentId=currentScreenId();

    if(currentId==="screen-dashboard" && id!=="screen-dashboard"){
      resetDashboardChart();
    }

    saveCurrentScroll();

    $$(".screen").forEach(s=>s.classList.remove("active"));
    $("#"+id).classList.add("active");

    requestAnimationFrame(()=>{
      const targetY=restoreScroll ? (state.scrollPositions[id] || 0) : 0;
      window.scrollTo(0,targetY);
    });
  }

  $$("[data-go]").forEach(b=>b.addEventListener("click",()=>{
    const label=(b.getAttribute("aria-label") || "").toLowerCase();
    showScreen(b.dataset.go,label.includes("voltar"));
  }));

  function toast(msg){
    const t=$("#toast");
    t.textContent=msg;
    t.classList.remove("hidden");
    clearTimeout(toast._timer);
    toast._timer=setTimeout(()=>t.classList.add("hidden"),1800);
  }

  function setOverlay(id, visible){
    const el=$("#"+id);
    el.classList.toggle("hidden",!visible);
    el.setAttribute("aria-hidden", visible ? "false":"true");
  }

  function clearErrors(form){
    form.querySelectorAll(".field-error").forEach(e=>e.textContent="");
  }
  function errorFor(id,msg){
    const e=document.querySelector(`[data-error-for="${id}"]`);
    if(e) e.textContent=msg;
  }
  function validPositive(value){ return Number.isFinite(value) && value > 0; }
  function clampDay(v){ return Math.max(1,Math.min(31,Number(v)||1)); }

  // ---------- Simulated database / recurrence engine ----------
  function recurrenceVersions(rec){
    return (rec.versions || []).slice().sort((a,b)=>a.from.localeCompare(b.from));
  }
  function versionFor(rec, period){
    if(comparePeriod(period,rec.start)<0) return null;
    if(rec.end && comparePeriod(period,rec.end)>0) return null;
    let version={from:rec.start, name:rec.name, value:rec.value, category:rec.category, day:rec.day};
    for(const v of recurrenceVersions(rec)){
      if(comparePeriod(v.from,period)<=0) version={...version,...v};
    }
    return version;
  }
  function exceptionFor(recId,period){
    return state.exceptions.find(e=>e.recurrenceId===recId && e.period===period) || null;
  }
  function recurringMovement(rec, period){
    const version=versionFor(rec,period);
    if(!version) return null;
    const ex=exceptionFor(rec.id,period);
    if(ex?.excluded) return null;
    const effective={...version,...(ex?.overrides||{})};
    return {
      id:`rec:${rec.id}:${period}`,
      sourceId:rec.id,
      type:rec.type,
      name:effective.name,
      value:Number(effective.value),
      category:rec.type==="expense" ? (effective.category || "Outros") : null,
      date:`${period}-${String(Math.min(effective.day||1,28)).padStart(2,"0")}`,
      recurring:true,
      period
    };
  }
  function movementsForPeriod(period,type){
    const normal=(type==="income"?state.incomes:state.expenses)
      .filter(m=>periodFromDate(m.date)===period)
      .map(m=>({...m, recurring:false, period}));
    const recs=state.recurrences
      .filter(r=>r.type===type)
      .map(r=>recurringMovement(r,period))
      .filter(Boolean);
    return [...normal,...recs].sort((a,b)=>a.date.localeCompare(b.date));
  }
  function analyze(period){
    const incomes=movementsForPeriod(period,"income");
    const expenses=movementsForPeriod(period,"expense");

    const incomeTotal=incomes.reduce((s,m)=>s+m.value,0);

    // Despesas registradas = apenas gastos pontuais informados como ocorridos.
    const registeredExpenses=expenses.filter(m=>!m.recurring);
    const registeredExpenseTotal=registeredExpenses.reduce((s,m)=>s+m.value,0);

    // Previsão do mês = gastos já registrados + recorrências conhecidas válidas.
    const forecastExpenseTotal=expenses.reduce((s,m)=>s+m.value,0);

    const diff=incomeTotal-forecastExpenseTotal;
    const ratio=incomeTotal>0 ? (forecastExpenseTotal/incomeTotal)*100 : null;

    const byCategory={};
    expenses.forEach(m=>byCategory[m.category]=(byCategory[m.category]||0)+m.value);

    return {
      incomes,
      expenses,
      registeredExpenses,
      incomeTotal,
      registeredExpenseTotal,
      forecastExpenseTotal,
      diff,
      ratio,
      byCategory
    };
  }

  // ---------- Dashboard ----------
  function renderDashboard(){
    const a=analyze(state.selectedPeriod);
    $("#period-label").textContent=periodLabel(state.selectedPeriod);

    const historicalMode = state.viewingHistoricalPeriod;
    $("#header-add").classList.toggle("hidden", historicalMode);
    $("#dashboard-history-back").classList.toggle("hidden", !historicalMode);
    $("#open-history").classList.toggle("hidden", historicalMode);
    $("#dashboard-header-spacer").classList.toggle("hidden", !historicalMode);

    const empty=a.incomes.length===0 && a.expenses.length===0;
    $("#dashboard-empty").classList.toggle("hidden",!empty);
    $("#dashboard-content").classList.toggle("hidden",empty);
    if(empty) return;

    $("#income-total").textContent=money(a.incomeTotal);
    $("#registered-expense-total").textContent=money(a.registeredExpenseTotal);
    $("#forecast-expense-total").textContent=money(a.forecastExpenseTotal);

    if(a.incomeTotal>0){
      if(a.diff>=0){
        $("#difference-label").textContent="Sobra prevista da renda";
        $("#difference-total").textContent=money(a.diff);
      } else {
        $("#difference-label").textContent="Despesas previstas acima da renda";
        $("#difference-total").textContent=money(Math.abs(a.diff));
      }
      $("#ratio-message").innerHTML=`A previsão de despesas representa <strong>${Math.round(a.ratio)}%</strong> da sua renda neste mês.`;
    } else {
      $("#difference-label").textContent="Renda não informada";
      $("#difference-total").textContent="—";
      $("#ratio-message").textContent="Sem renda informada, não calculamos percentual nem sobra prevista.";
    }

    // Mostra a composição da renda diretamente no Dashboard.
    $("#income-source-list").innerHTML=a.incomes.length
      ? a.incomes.map((income,index)=>`
          <button class="category-row income-source-row" data-income-index="${index}">
            <span>
              <strong>${income.name}</strong><br>
              <small>${income.recurring ? "Mensal" : "Registrada no mês"}</small>
            </span>
            <strong>${money(income.value)}</strong>
          </button>
        `).join("")
      : `<p class="muted analysis-empty">Nenhuma renda informada neste mês.</p>`;

    $$("#income-source-list [data-income-index]").forEach(button=>{
      button.addEventListener("click",()=>{
        const income=a.incomes[Number(button.dataset.incomeIndex)];
        if(income){
          openIncomeSourceList(income.name);
        }
      });
    });

    const entries=Object.entries(a.byCategory).sort((x,y)=>y[1]-x[1]);
    $("#category-list").innerHTML=entries.length
      ? entries.map(([cat,total])=>`
          <button class="category-row" data-category="${cat}">
            <span>
              <strong>${cat}</strong><br>
              <small>${a.expenses.filter(e=>e.category===cat).length} movimentação(ões)</small>
            </span>
            <strong>${money(total)}</strong>
          </button>
        `).join("")
      : `<p class="muted analysis-empty">Nenhuma despesa prevista neste mês.</p>`;

    $$("#category-list [data-category]").forEach(b=>b.addEventListener("click",()=>openList("expense",b.dataset.category)));

    renderExpenseChart(a);
  }

  function renderExpenseChart(a){
    const chart=$("#expense-chart");
    const entries=Object.entries(a.byCategory).sort((x,y)=>y[1]-x[1]);

    if(!entries.length){
      chart.innerHTML=`<p class="chart-empty">Nenhuma despesa disponível para gerar o gráfico neste período.</p>`;
      return;
    }

    const total=entries.reduce((sum,[,value])=>sum+value,0);

    chart.innerHTML=entries.map(([category,value])=>{
      const share=total>0 ? (value/total)*100 : 0;
      const roundedShare=Math.round(share);

      return `
        <div class="chart-row">
          <div class="chart-row-top">
            <div>
              <div class="chart-label">${category}</div>
              <div class="chart-meta">${roundedShare}% do total das despesas</div>
            </div>
            <div class="chart-value">${money(value)}</div>
          </div>
          <div class="chart-track" aria-hidden="true">
            <div class="chart-fill" style="width:${share}%;"></div>
          </div>
        </div>
      `;
    }).join("");
  }

  function openDashboard(restoreScroll=false){
    renderDashboard();
    showScreen("screen-dashboard",restoreScroll);
  }

  // ---------- Histórico ----------
  function periodHasData(period){
    const a=analyze(period);
    return a.incomes.length>0 || a.expenses.length>0;
  }

  function historyPeriodsForYear(year){
    const currentYear=Number(PROTOTYPE_CURRENT_PERIOD.slice(0,4));
    const currentMonth=Number(PROTOTYPE_CURRENT_PERIOD.slice(5,7));

    const periods=[];
    for(let month=12; month>=1; month--){
      if(year>currentYear) continue;
      if(year===currentYear && month>currentMonth) continue;

      const period=`${year}-${String(month).padStart(2,"0")}`;
      if(periodHasData(period)){
        periods.push(period);
      }
    }
    return periods;
  }

  function renderHistoryYearChart(periods){
    const container=$("#history-year-chart");

    if(!periods.length){
      container.innerHTML=`<p class="history-chart-empty">Nenhum período com informações para gerar o gráfico em ${state.pickerYear}.</p>`;
      return;
    }

    const data=periods
      .slice()
      .reverse()
      .map(period=>{
        const a=analyze(period);
        return {
          period,
          label: shortMonthNames[Number(period.slice(5,7))-1],
          value: a.forecastExpenseTotal
        };
      });

    const total=data.reduce((acc,item)=>acc + item.value, 0);

    container.innerHTML=data.map(item=>{
      const share=total > 0 ? (item.value / total) * 100 : 0;
      return `
        <div class="history-chart-row">
          <div class="history-chart-row-top">
            <div>
              <div class="history-chart-label">${item.label}</div>
              <div class="history-chart-meta">Previsão de despesas do mês</div>
            </div>
            <div class="history-chart-value">${money(item.value)}</div>
          </div>
          <div class="history-chart-track" aria-hidden="true">
            <div class="history-chart-fill" style="width:${share}%;"></div>
          </div>
        </div>
      `;
    }).join("");
  }

  function renderHistory(){
    $("#history-year").textContent=state.pickerYear;

    const periods=historyPeriodsForYear(state.pickerYear);
    const list=$("#history-list");

    const chartCard=$("#history-chart-card");
    const chartButton=$("#toggle-history-chart-btn");

    chartCard.classList.toggle("hidden", !state.historyYearChartOpen);
    chartButton.setAttribute("aria-expanded", String(state.historyYearChartOpen));
    chartButton.textContent=state.historyYearChartOpen ? "Ocultar evolução do ano" : "Ver evolução do ano";

    if(state.historyYearChartOpen){
      renderHistoryYearChart(periods);
    }

    if(periods.length===0){
      list.innerHTML=`
        <div class="history-empty">
          Nenhum período com informações em ${state.pickerYear}.
        </div>
      `;
      return;
    }

    list.innerHTML=periods.map(period=>{
      const a=analyze(period);
      const isCurrent=period===PROTOTYPE_CURRENT_PERIOD;

      let differenceLabel;
      let differenceValue;

      if(a.incomeTotal<=0){
        differenceLabel="Sobra prevista";
        differenceValue="—";
      } else if(a.diff>=0){
        differenceLabel="Sobra prevista";
        differenceValue=money(a.diff);
      } else {
        differenceLabel="Acima da renda";
        differenceValue=money(Math.abs(a.diff));
      }

      return `
        <button class="history-card ${isCurrent?"current":""}" data-period="${period}">
          <div class="history-card-header">
            <strong>${periodLabel(period)}</strong>
            ${isCurrent?'<span class="current-chip">Atual</span>':""}
          </div>

          <div class="history-summary">
            <div class="history-summary-row">
              <span>Renda</span>
              <strong>${money(a.incomeTotal)}</strong>
            </div>
            <div class="history-summary-row">
              <span>Previsão de despesas</span>
              <strong>${money(a.forecastExpenseTotal)}</strong>
            </div>
            <div class="history-summary-row">
              <span>${differenceLabel}</span>
              <strong>${differenceValue}</strong>
            </div>
          </div>
        </button>
      `;
    }).join("");

    $$("#history-list [data-period]").forEach(button=>{
      button.addEventListener("click",()=>{
        state.selectedPeriod=button.dataset.period;
        state.viewingHistoricalPeriod = button.dataset.period !== PROTOTYPE_CURRENT_PERIOD;
        openDashboard();
      });
    });
  }

  $("#open-history").addEventListener("click",()=>{
    state.viewingHistoricalPeriod=false;
    state.historyYearChartOpen=false;
    state.pickerYear=Number(state.selectedPeriod.slice(0,4));
    renderHistory();
    showScreen("screen-history");
  });

  $("#history-back").addEventListener("click",()=>{
    state.selectedPeriod=PROTOTYPE_CURRENT_PERIOD;
    state.viewingHistoricalPeriod=false;
    state.historyYearChartOpen=false;
    openDashboard(true);
  });

  $("#history-prev-year").addEventListener("click",()=>{
    state.pickerYear--;
    renderHistory();
  });

  $("#history-next-year").addEventListener("click",()=>{
    state.pickerYear++;
    renderHistory();
  });

  $("#dashboard-history-back").addEventListener("click",()=>{
    state.pickerYear=Number(state.selectedPeriod.slice(0,4));
    state.viewingHistoricalPeriod=false;
    renderHistory();
    showScreen("screen-history",true);
  });

  $("#toggle-history-chart-btn").addEventListener("click",()=>{
    state.historyYearChartOpen=!state.historyYearChartOpen;
    renderHistory();
  });

  $("#toggle-chart-btn").addEventListener("click",()=>{
    const card=$("#chart-card");
    const button=$("#toggle-chart-btn");
    const isOpen=!card.classList.contains("hidden");

    if(isOpen){
      card.classList.add("hidden");
      button.textContent="Ver gráfico das despesas";
      button.setAttribute("aria-expanded","false");
    } else {
      card.classList.remove("hidden");
      button.textContent="Ocultar gráfico";
      button.setAttribute("aria-expanded","true");
    }
  });

  // ---------- Add sheet ----------
  $("#header-add").addEventListener("click",()=>{
    if(state.viewingHistoricalPeriod) return;
    setOverlay("sheet-backdrop",true);
  });
  $("#close-add-sheet").addEventListener("click",()=>setOverlay("sheet-backdrop",false));
  $("#choose-expense").addEventListener("click",()=>{setOverlay("sheet-backdrop",false);openExpenseForm();});
  $("#choose-income").addEventListener("click",()=>{setOverlay("sheet-backdrop",false);openIncomeForm();});

  // ---------- First access ----------
  $("#explore-first").addEventListener("click",()=>{
    state.selectedPeriod="2026-09";
    state.viewingHistoricalPeriod=false;
    openDashboard();
  });

  $("#first-income-recurring").addEventListener("change",(e)=>{
    $("#first-income-oneoff").classList.toggle("hidden",e.target.checked);
    $("#first-income-recurring-fields").classList.toggle("hidden",!e.target.checked);
  });

  function seedFormDates(){
    $("#first-income-date").value=todayInPeriod(state.selectedPeriod,5);
    $("#first-income-start").value=state.selectedPeriod;
  }
  seedFormDates();

  $("#first-income-form").addEventListener("submit",(e)=>{
    e.preventDefault();
    clearErrors(e.currentTarget);
    const name=$("#first-income-name").value.trim();
    const value=Number($("#first-income-value").value);
    const recurring=$("#first-income-recurring").checked;
    let ok=true;
    if(!name){errorFor("first-income-name","Informe a origem da renda.");ok=false;}
    if(!validPositive(value)){errorFor("first-income-value","Informe um valor maior que R$0.");ok=false;}
    if(!ok) return;

    if(recurring){
      const start=$("#first-income-start").value || state.selectedPeriod;
      state.recurrences.push({id:uid(),type:"income",name,value,start,day:clampDay($("#first-income-day").value),end:null,versions:[]});
      state.selectedPeriod=start;
    } else {
      const date=$("#first-income-date").value || todayInPeriod(state.selectedPeriod,5);
      state.incomes.push({id:uid(),type:"income",name,value,date});
      state.selectedPeriod=periodFromDate(date);
    }
    renderOnboardingExpenses();
    showScreen("screen-onboarding-expenses");
  });

  function renderOnboardingExpenses(){
    const items=state.recurrences.filter(r=>r.type==="expense" && !r.end);
    $("#onboarding-recurring-list").innerHTML=items.length
      ? items.map(r=>`<div class="movement-row"><span><strong>${r.name}</strong><div class="meta"><span class="badge">${r.category}</span><span class="badge">Mensal</span></div></span><strong>${money(r.value)}</strong></div>`).join("")
      : `<p class="muted">Nenhum gasto mensal adicionado ainda.</p>`;
  }
  $("#add-onboarding-recurring").addEventListener("click",()=>{
    state.onboardingExpenseMode=true;
    state.returnScreen="screen-onboarding-expenses";
    openExpenseForm({forceRecurring:true});
  });
  $("#finish-onboarding").addEventListener("click",()=>{state.onboardingExpenseMode=false;openDashboard();});

  // ---------- Form toggles ----------
  $("#expense-recurring").addEventListener("change",(e)=>{
    $("#expense-oneoff-fields").classList.toggle("hidden",e.target.checked);
    $("#expense-recurring-fields").classList.toggle("hidden",!e.target.checked);
  });
  $("#income-recurring").addEventListener("change",(e)=>{
    $("#income-oneoff-fields").classList.toggle("hidden",e.target.checked);
    $("#income-recurring-fields").classList.toggle("hidden",!e.target.checked);
  });
  $$(".back-context").forEach(b=>b.addEventListener("click",()=>showScreen(state.returnScreen,true)));

  function resetExpenseForm(){
    $("#expense-form").reset();
    clearErrors($("#expense-form"));
    $("#expense-date").value=todayInPeriod(state.selectedPeriod,4);
    $("#expense-start").value=state.selectedPeriod;
    $("#expense-day").value=10;
    $("#expense-recurring").checked=false;
    $("#expense-oneoff-fields").classList.remove("hidden");
    $("#expense-recurring-fields").classList.add("hidden");
    $("#expense-recurring-row").classList.remove("hidden");
    $("#expense-form-title").textContent="Novo gasto";
    $("#expense-save").textContent="Salvar gasto";
  }

  function openExpenseForm(opts={}){
    resetExpenseForm();
    state.editing=opts.editing || null;
    state.returnScreen=opts.returnScreen || state.returnScreen || "screen-dashboard";

    if(opts.forceRecurring){
      $("#expense-recurring").checked=true;
      $("#expense-oneoff-fields").classList.add("hidden");
      $("#expense-recurring-fields").classList.remove("hidden");
    }

    if(opts.editing){
      const m=opts.editing;
      $("#expense-form-title").textContent="Editar gasto";
      $("#expense-save").textContent="Salvar alteração";
      $("#expense-value").value=m.value;
      $("#expense-description").value=m.name;
      $("#expense-category").value=m.category;
      if(m.recurring){
        $("#expense-recurring").checked=true;
        $("#expense-recurring-row").classList.add("hidden");
        $("#expense-oneoff-fields").classList.add("hidden");
        $("#expense-recurring-fields").classList.add("hidden");
      } else {
        $("#expense-date").value=m.date;
      }
    }
    showScreen("screen-expense-form");
  }

  function resetIncomeForm(){
    $("#income-form").reset();
    clearErrors($("#income-form"));
    $("#income-date").value=todayInPeriod(state.selectedPeriod,4);
    $("#income-start").value=state.selectedPeriod;
    $("#income-day").value=5;
    $("#income-recurring").checked=false;
    $("#income-oneoff-fields").classList.remove("hidden");
    $("#income-recurring-fields").classList.add("hidden");
    $("#income-recurring-row").classList.remove("hidden");
    $("#income-form-title").textContent="Nova renda";
    $("#income-save").textContent="Salvar renda";
  }

  function openIncomeForm(opts={}){
    resetIncomeForm();
    state.editing=opts.editing || null;
    state.returnScreen=opts.returnScreen || "screen-dashboard";
    if(opts.editing){
      const m=opts.editing;
      $("#income-form-title").textContent="Editar renda";
      $("#income-save").textContent="Salvar alteração";
      $("#income-name").value=m.name;
      $("#income-value").value=m.value;
      if(m.recurring){
        $("#income-recurring").checked=true;
        $("#income-recurring-row").classList.add("hidden");
        $("#income-oneoff-fields").classList.add("hidden");
        $("#income-recurring-fields").classList.add("hidden");
      } else {
        $("#income-date").value=m.date;
      }
    }
    showScreen("screen-income-form");
  }

  // ---------- Save forms ----------
  $("#expense-form").addEventListener("submit",(e)=>{
    e.preventDefault();
    clearErrors(e.currentTarget);
    const value=Number($("#expense-value").value);
    const name=$("#expense-description").value.trim();
    const category=$("#expense-category").value;
    let ok=true;
    if(!validPositive(value)){errorFor("expense-value","Informe um valor maior que R$0.");ok=false;}
    if(!name){errorFor("expense-description","Informe uma descrição.");ok=false;}
    if(!ok) return;

    if(state.editing){
      const m=state.editing;
      if(m.recurring){
        state.pendingScopeSave={type:"expense",sourceId:m.sourceId,period:state.selectedPeriod,changes:{name,value,category}};
        openScopeSheet();
        return;
      } else {
        const original=state.expenses.find(x=>x.id===m.id);
        if(original){Object.assign(original,{name,value,category,date:$("#expense-date").value});}
        toast("Gasto atualizado");
        openList("expense",state.listContext.category,true);
        return;
      }
    }

    if($("#expense-recurring").checked){
      state.recurrences.push({
        id:uid(),type:"expense",name,value,category,
        start:$("#expense-start").value || state.selectedPeriod,
        day:clampDay($("#expense-day").value),end:null,versions:[]
      });
    } else {
      state.expenses.push({id:uid(),type:"expense",name,value,category,date:$("#expense-date").value || todayInPeriod(state.selectedPeriod,4)});
    }

    toast("Gasto adicionado");
    if(state.onboardingExpenseMode){
      renderOnboardingExpenses();
      showScreen("screen-onboarding-expenses");
    } else {
      openDashboard();
    }
  });

  $("#income-form").addEventListener("submit",(e)=>{
    e.preventDefault();
    clearErrors(e.currentTarget);
    const name=$("#income-name").value.trim();
    const value=Number($("#income-value").value);
    let ok=true;
    if(!name){errorFor("income-name","Informe a origem da renda.");ok=false;}
    if(!validPositive(value)){errorFor("income-value","Informe um valor maior que R$0.");ok=false;}
    if(!ok) return;

    if(state.editing){
      const m=state.editing;
      if(m.recurring){
        state.pendingScopeSave={type:"income",sourceId:m.sourceId,period:state.selectedPeriod,changes:{name,value}};
        openScopeSheet();
        return;
      } else {
        const original=state.incomes.find(x=>x.id===m.id);
        if(original){Object.assign(original,{name,value,date:$("#income-date").value});}
        toast("Renda atualizada");
        if(state.listContext.type==="income-source"){
          openIncomeSourceList(state.listContext.sourceName || name,true);
        } else {
          openList("income",null,true);
        }
        return;
      }
    }

    if($("#income-recurring").checked){
      state.recurrences.push({id:uid(),type:"income",name,value,start:$("#income-start").value || state.selectedPeriod,day:clampDay($("#income-day").value),end:null,versions:[]});
    } else {
      state.incomes.push({id:uid(),type:"income",name,value,date:$("#income-date").value || todayInPeriod(state.selectedPeriod,4)});
    }
    toast("Renda adicionada");
    openDashboard();
  });

  // ---------- Lista por origem de renda ----------
  function openIncomeSourceList(sourceName, restoreScroll=false){
    state.listContext={type:"income-source",category:null,sourceName};

    const items=movementsForPeriod(state.selectedPeriod,"income")
      .filter(m=>m.name===sourceName);

    $("#list-eyebrow").textContent=periodLabel(state.selectedPeriod);
    $("#list-title").textContent=sourceName;

    $("#movement-list").innerHTML=items.length
      ? items.map(m=>`
        <button class="movement-row clickable-row" data-id="${m.id}">
          <span>
            <strong>${m.name}</strong>
            <div class="meta">
              ${m.recurring?`<span class="badge">Mensal</span>`:`<span class="badge">Registrada no mês</span>`}
            </div>
          </span>
          <span class="movement-row-end">
            <strong>${money(m.value)}</strong>
            <svg class="row-chevron" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M9 5.5L15.5 12 9 18.5"></path>
            </svg>
          </span>
        </button>`).join("")
      : `<div class="empty-state">
           <div class="empty-icon">○</div>
           <h2>Nenhuma movimentação</h2>
           <p>Não há rendas dessa origem neste período.</p>
         </div>`;

    $$("#movement-list [data-id]").forEach(button=>{
      button.addEventListener("click",()=>{
        const movement=items.find(x=>x.id===button.dataset.id);
        if(movement) openDetail(movement);
      });
    });

    showScreen("screen-list",restoreScroll);
  }

  // ---------- Lists ----------
  function openList(type,category=null,restoreScroll=false){
    state.listContext={type,category};

    let items;
    let title;

    if(type==="registered-expense"){
      items=movementsForPeriod(state.selectedPeriod,"expense")
        .filter(m=>!m.recurring)
        .filter(m=>!category || m.category===category);
      title=category || "Despesas registradas";
    } else {
      items=movementsForPeriod(state.selectedPeriod,type)
        .filter(m=>!category || m.category===category);
      title=category || (type==="income" ? "Rendas":"Previsão de despesas");
    }

    $("#list-eyebrow").textContent=periodLabel(state.selectedPeriod);
    $("#list-title").textContent=title;
    $("#movement-list").innerHTML=items.length
      ? items.map(m=>`
        <button class="movement-row clickable-row" data-id="${m.id}">
          <span>
            <strong>${m.name}</strong>
            <div class="meta">
              ${m.category?`<span class="badge">${m.category}</span>`:""}
              ${m.recurring?`<span class="badge">Mensal</span>`:""}
            </div>
          </span>
          <span class="movement-row-end">
            <strong>${money(m.value)}</strong>
            <svg class="row-chevron" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M9 5.5L15.5 12 9 18.5"></path>
            </svg>
          </span>
        </button>`).join("")
      : `<div class="empty-state"><div class="empty-icon">○</div><h2>Nenhuma movimentação</h2><p>Não há itens para mostrar neste período.</p></div>`;
    $$("#movement-list [data-id]").forEach(b=>b.addEventListener("click",()=>{
      const m=items.find(x=>x.id===b.dataset.id);
      openDetail(m);
    }));
    showScreen("screen-list",restoreScroll);
  }

  // ---------- Detail ----------
  function openDetail(m){
    state.detailRef=m;
    $("#detail-content").innerHTML=`
      <div class="detail-card"><span>${m.type==="income"?"Origem":"Descrição"}</span><strong>${m.name}</strong></div>
      <div class="detail-card"><span>Valor</span><strong>${money(m.value)}</strong></div>
      ${m.category?`<div class="detail-card"><span>Categoria</span><strong>${m.category}</strong></div>`:""}
      <div class="detail-card"><span>Período</span><strong>${periodLabel(state.selectedPeriod)}</strong></div>
      <div class="detail-card"><span>Tipo</span><strong>${m.recurring?"Mensal":"Pontual"}</strong></div>
    `;
    $("#stop-recurring").classList.toggle("hidden",!m.recurring);
    showScreen("screen-detail");
  }
  $("#detail-back").addEventListener("click",()=>{
    if(state.listContext.type==="income-source"){
      openIncomeSourceList(state.listContext.sourceName,true);
    } else {
      openList(state.listContext.type,state.listContext.category,true);
    }
  });
  $("#edit-movement").addEventListener("click",()=>{
    const m=state.detailRef;
    if(!m) return;
    if(m.type==="expense") openExpenseForm({editing:m,returnScreen:"screen-detail"});
    else openIncomeForm({editing:m,returnScreen:"screen-detail"});
  });

  // ---------- Recurrence edit scope ----------
  function openScopeSheet(){
    const p=periodLabel(state.pendingScopeSave.period);
    $("#scope-description").textContent="Escolha em quais períodos essa alteração deve valer.";
    $("#scope-this-month").innerHTML=`<span><strong>Somente em ${p}</strong><small>Os outros meses continuam como antes.</small></span>`;
    $("#scope-from-month").innerHTML=`<span><strong>A partir de ${p}</strong><small>O histórico anterior permanece inalterado.</small></span>`;
    setOverlay("scope-backdrop",true);
  }
  $("#scope-cancel").addEventListener("click",()=>setOverlay("scope-backdrop",false));
  $("#scope-this-month").addEventListener("click",()=>{
    const p=state.pendingScopeSave;
    const existing=exceptionFor(p.sourceId,p.period);
    if(existing){existing.overrides={...(existing.overrides||{}),...p.changes};existing.excluded=false;}
    else state.exceptions.push({id:uid(),recurrenceId:p.sourceId,period:p.period,excluded:false,overrides:{...p.changes}});
    state.pendingScopeSave=null;
    setOverlay("scope-backdrop",false);
    toast("Alteração aplicada somente ao mês");
    if(state.listContext.type==="income-source"){
      openIncomeSourceList(state.listContext.sourceName,true);
    } else {
      openList(state.listContext.type,state.listContext.category,true);
    }
  });
  $("#scope-from-month").addEventListener("click",()=>{
    const p=state.pendingScopeSave;
    const rec=state.recurrences.find(r=>r.id===p.sourceId);
    if(rec){
      rec.versions=rec.versions||[];
      rec.versions=rec.versions.filter(v=>v.from!==p.period);
      rec.versions.push({from:p.period,...p.changes});
    }
    state.pendingScopeSave=null;
    setOverlay("scope-backdrop",false);
    toast("Alteração aplicada aos próximos meses");
    if(state.listContext.type==="income-source"){
      openIncomeSourceList(state.listContext.sourceName,true);
    } else {
      openList(state.listContext.type,state.listContext.category,true);
    }
  });

  // ---------- Stop recurrence ----------
  $("#stop-recurring").addEventListener("click",()=>{
    const m=state.detailRef;
    if(!m?.recurring) return;
    const p=periodLabel(state.selectedPeriod);
    $("#stop-description").textContent=`Escolha como "${m.name}" deve ficar em ${p}.`;
    $("#stop-keep-current").innerHTML=`<span><strong>Manter em ${p}</strong><small>Parar a partir de ${periodLabel(nextPeriod(state.selectedPeriod))}.</small></span>`;
    $("#stop-remove-current").innerHTML=`<span><strong>Remover de ${p}</strong><small>Parar a partir deste mês.</small></span>`;
    setOverlay("stop-backdrop",true);
  });
  $("#stop-cancel").addEventListener("click",()=>setOverlay("stop-backdrop",false));
  $("#stop-keep-current").addEventListener("click",()=>{
    const m=state.detailRef;
    const rec=state.recurrences.find(r=>r.id===m.sourceId);
    if(rec) rec.end=state.selectedPeriod;
    setOverlay("stop-backdrop",false);
    toast("Recorrência interrompida");
    openDashboard();
  });
  $("#stop-remove-current").addEventListener("click",()=>{
    const m=state.detailRef;
    const rec=state.recurrences.find(r=>r.id===m.sourceId);
    if(rec) rec.end=prevPeriod(state.selectedPeriod);
    setOverlay("stop-backdrop",false);
    toast("Recorrência interrompida");
    openDashboard();
  });

  // ---------- Delete ----------
  $("#delete-movement").addEventListener("click",()=>{
    const m=state.detailRef;
    if(!m) return;
    state.pendingDelete=m;
    $("#confirm-title").textContent=`Excluir ${m.type==="expense"?"este gasto":"esta renda"}?`;
    $("#confirm-message").textContent=`${m.name} — ${money(m.value)}. Essa ação removerá a movimentação da análise de ${periodLabel(state.selectedPeriod)}.`;
    setOverlay("confirm-backdrop",true);
  });
  $("#confirm-cancel").addEventListener("click",()=>setOverlay("confirm-backdrop",false));
  $("#confirm-yes").addEventListener("click",()=>{
    const m=state.pendingDelete;
    if(!m) return;
    if(m.recurring){
      // Delete only the selected month's occurrence; "Parar recorrência" is the future-facing action.
      const existing=exceptionFor(m.sourceId,state.selectedPeriod);
      if(existing){existing.excluded=true;existing.overrides={};}
      else state.exceptions.push({id:uid(),recurrenceId:m.sourceId,period:state.selectedPeriod,excluded:true,overrides:{}});
    } else if(m.type==="expense"){
      state.expenses=state.expenses.filter(x=>x.id!==m.id);
    } else {
      state.incomes=state.incomes.filter(x=>x.id!==m.id);
    }
    state.pendingDelete=null;
    setOverlay("confirm-backdrop",false);
    toast("Movimentação excluída");
    if(state.listContext.type==="income-source"){
      openIncomeSourceList(state.listContext.sourceName,true);
    } else {
      openList(state.listContext.type,state.listContext.category,true);
    }
  });

  // ---------- Demo data helper for Explore ----------
  function loadDemoIfEmpty(){
    if(state.incomes.length||state.expenses.length||state.recurrences.length) return;
    state.recurrences.push(
      {id:uid(),type:"income",name:"Salário",value:10000,start:"2026-09",day:5,end:null,versions:[]},
      {id:uid(),type:"expense",name:"Aluguel",value:2000,category:"Moradia",start:"2026-09",day:10,end:null,versions:[]},
      {id:uid(),type:"expense",name:"Internet",value:120,category:"Moradia",start:"2026-09",day:12,end:null,versions:[]},
      {id:uid(),type:"expense",name:"Academia",value:100,category:"Saúde",start:"2026-09",day:8,end:null,versions:[]}
    );
    state.expenses.push(
      {id:uid(),type:"expense",name:"Mercado",value:650,category:"Alimentação",date:"2026-09-11"},
      {id:uid(),type:"expense",name:"Restaurante",value:180,category:"Alimentação",date:"2026-09-14"},
      {id:uid(),type:"expense",name:"Uber",value:150,category:"Transporte",date:"2026-09-16"},
      {id:uid(),type:"expense",name:"Cinema",value:100,category:"Lazer",date:"2026-09-19"},

      // Dados fictícios anteriores apenas para testar a tela de Histórico.
      {id:uid(),type:"expense",name:"Mercado",value:720,category:"Alimentação",date:"2026-08-12"},
      {id:uid(),type:"expense",name:"Transporte",value:260,category:"Transporte",date:"2026-08-18"},
      {id:uid(),type:"expense",name:"Lazer",value:190,category:"Lazer",date:"2026-08-22"},
      {id:uid(),type:"expense",name:"Mercado",value:610,category:"Alimentação",date:"2026-07-10"},
      {id:uid(),type:"expense",name:"Saúde",value:230,category:"Saúde",date:"2026-07-20"}
    );

    state.incomes.push(
      {id:uid(),type:"income",name:"Salário",value:10000,date:"2026-08-05"},
      {id:uid(),type:"income",name:"Salário",value:10000,date:"2026-07-05"}
    );
  }
  $("#explore-first").addEventListener("click",()=>{
    loadDemoIfEmpty();
    state.selectedPeriod=PROTOTYPE_CURRENT_PERIOD;
    state.viewingHistoricalPeriod=false;
    state.historyYearChartOpen=false;
    openDashboard();
  });

  // Initialize
  renderDashboard();
})();
