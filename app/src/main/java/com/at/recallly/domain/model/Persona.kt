package com.at.recallly.domain.model

enum class Persona(val displayName: String, val description: String) {
    SALES_REP("Sales Rep", "Track deals, clients, and follow-ups"),
    FIELD_ENGINEER("Field Engineer", "Log site visits, equipment, and issues"),
    INSURANCE_ADJUSTER("Insurance Adjuster", "Record claims, damages, and inspections")
}
