package com.at.recallly.core.util

import android.content.Context
import com.at.recallly.R
import com.at.recallly.domain.model.Persona

object FieldLocalizer {

    fun getLocalizedFieldName(context: Context, fieldId: String): String = when (fieldId) {
        // Sales Rep
        "sr_client_name" -> context.getString(R.string.field_sr_client_name)
        "sr_company" -> context.getString(R.string.field_sr_company)
        "sr_deal_value" -> context.getString(R.string.field_sr_deal_value)
        "sr_pain_points" -> context.getString(R.string.field_sr_pain_points)
        "sr_interest_level" -> context.getString(R.string.field_sr_interest_level)
        "sr_competitive" -> context.getString(R.string.field_sr_competitive)
        "sr_next_action" -> context.getString(R.string.field_sr_next_action)
        "sr_followup_date" -> context.getString(R.string.field_sr_followup_date)
        // Field Engineer
        "fe_site_name" -> context.getString(R.string.field_fe_site_name)
        "fe_asset_id" -> context.getString(R.string.field_fe_asset_id)
        "fe_issue_desc" -> context.getString(R.string.field_fe_issue_desc)
        "fe_parts" -> context.getString(R.string.field_fe_parts)
        "fe_priority" -> context.getString(R.string.field_fe_priority)
        "fe_safety" -> context.getString(R.string.field_fe_safety)
        "fe_time_est" -> context.getString(R.string.field_fe_time_est)
        "fe_deadline" -> context.getString(R.string.field_fe_deadline)
        // Insurance Adjuster
        "ia_policy" -> context.getString(R.string.field_ia_policy)
        "ia_claimant" -> context.getString(R.string.field_ia_claimant)
        "ia_incident_type" -> context.getString(R.string.field_ia_incident_type)
        "ia_damage" -> context.getString(R.string.field_ia_damage)
        "ia_liability" -> context.getString(R.string.field_ia_liability)
        "ia_repair_cost" -> context.getString(R.string.field_ia_repair_cost)
        "ia_evidence" -> context.getString(R.string.field_ia_evidence)
        "ia_inspection_date" -> context.getString(R.string.field_ia_inspection_date)
        else -> fieldId
    }

    fun getLocalizedPersonaName(context: Context, persona: Persona): String = when (persona) {
        Persona.SALES_REP -> context.getString(R.string.persona_sales_rep)
        Persona.FIELD_ENGINEER -> context.getString(R.string.persona_field_engineer)
        Persona.INSURANCE_ADJUSTER -> context.getString(R.string.persona_insurance_adjuster)
    }
}
