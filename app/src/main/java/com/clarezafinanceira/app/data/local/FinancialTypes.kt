package com.clarezafinanceira.app.data.local

// Storage codes are explicit: renaming a Kotlin constant must not change stored data.
enum class MovementType(val storageCode: String) {
    INCOME("INCOME"), EXPENSE("EXPENSE")
}

enum class ExpenseCategory(val storageCode: String) {
    FOOD("FOOD"), HOUSING("HOUSING"), TRANSPORT("TRANSPORT"),
    LEISURE("LEISURE"), HEALTH("HEALTH"), OTHER("OTHER")
}

internal fun validateCategory(type: MovementType, category: ExpenseCategory?) {
    require((type == MovementType.EXPENSE) == (category != null)) {
        "Expenses require a category; income must not have one"
    }
}
