package com.at.recallly.domain.model

object PersonaFields {

    private val salesRepFields = listOf(
        PersonaField("sr_client_name", "Client Name & Title", "Full name and job title", Persona.SALES_REP),
        PersonaField("sr_company", "Company Name", "Organization they represent", Persona.SALES_REP),
        PersonaField("sr_deal_value", "Deal Value", "Budget or contract size mentioned", Persona.SALES_REP),
        PersonaField("sr_pain_points", "Pain Points", "Problems they're trying to solve", Persona.SALES_REP),
        PersonaField("sr_interest_level", "Interest Level", "AI-inferred: Cold, Warm, or Hot", Persona.SALES_REP),
        PersonaField("sr_competitive", "Competitive Mention", "Any competitors discussed", Persona.SALES_REP),
        PersonaField("sr_next_action", "Next Action", "Agreed next step to take", Persona.SALES_REP),
        PersonaField("sr_followup_date", "Follow-up Date", "Scheduled follow-up for calendar", Persona.SALES_REP),
    )

    private val fieldEngineerFields = listOf(
        PersonaField("fe_site_name", "Site/Location Name", "Where the work is being done", Persona.FIELD_ENGINEER),
        PersonaField("fe_asset_id", "Asset/Equipment ID", "Specific machine or system tag", Persona.FIELD_ENGINEER),
        PersonaField("fe_issue_desc", "Issue Description", "Technical details of the problem", Persona.FIELD_ENGINEER),
        PersonaField("fe_parts", "Parts Required", "Hardware or tools needed", Persona.FIELD_ENGINEER),
        PersonaField("fe_priority", "Priority Level", "Low, Medium, High, or Emergency", Persona.FIELD_ENGINEER),
        PersonaField("fe_safety", "Safety Hazards", "Risks identified on-site", Persona.FIELD_ENGINEER),
        PersonaField("fe_time_est", "Time Estimate", "Expected duration for the fix", Persona.FIELD_ENGINEER),
        PersonaField("fe_deadline", "Deadline", "When the site must be operational", Persona.FIELD_ENGINEER),
    )

    private val insuranceAdjusterFields = listOf(
        PersonaField("ia_policy", "Policy Number", "Reference for the claim", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_claimant", "Claimant Name", "Person or business filing", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_incident_type", "Incident Type", "Fire, Flood, Collision, Theft, etc.", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_damage", "Damage Assessment", "Structured description of damage", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_liability", "Liability Notes", "Fault assessment from testimony", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_repair_cost", "Estimated Repair Cost", "Adjuster's spoken estimate", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_evidence", "Evidence Gathered", "Photos, witness statements, etc.", Persona.INSURANCE_ADJUSTER),
        PersonaField("ia_inspection_date", "Inspection Date", "Official time of the visit", Persona.INSURANCE_ADJUSTER),
    )

    fun getFieldsForPersona(persona: Persona): List<PersonaField> = when (persona) {
        Persona.SALES_REP -> salesRepFields
        Persona.FIELD_ENGINEER -> fieldEngineerFields
        Persona.INSURANCE_ADJUSTER -> insuranceAdjusterFields
    }
}
