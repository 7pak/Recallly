package com.at.recallly.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.at.recallly.R
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField

@Composable
fun Persona.localizedDisplayName(): String = stringResource(
    when (this) {
        Persona.SALES_REP -> R.string.persona_sales_rep
        Persona.FIELD_ENGINEER -> R.string.persona_field_engineer
        Persona.INSURANCE_ADJUSTER -> R.string.persona_insurance_adjuster
    }
)

@Composable
fun Persona.localizedDescription(): String = stringResource(
    when (this) {
        Persona.SALES_REP -> R.string.persona_sales_rep_desc
        Persona.FIELD_ENGINEER -> R.string.persona_field_engineer_desc
        Persona.INSURANCE_ADJUSTER -> R.string.persona_insurance_adjuster_desc
    }
)

@Composable
fun PersonaField.localizedDisplayName(): String = stringResource(
    when (id) {
        // Sales Rep
        "sr_client_name" -> R.string.field_sr_client_name
        "sr_company" -> R.string.field_sr_company
        "sr_deal_value" -> R.string.field_sr_deal_value
        "sr_pain_points" -> R.string.field_sr_pain_points
        "sr_interest_level" -> R.string.field_sr_interest_level
        "sr_competitive" -> R.string.field_sr_competitive
        "sr_next_action" -> R.string.field_sr_next_action
        "sr_followup_date" -> R.string.field_sr_followup_date
        // Field Engineer
        "fe_site_name" -> R.string.field_fe_site_name
        "fe_asset_id" -> R.string.field_fe_asset_id
        "fe_issue_desc" -> R.string.field_fe_issue_desc
        "fe_parts" -> R.string.field_fe_parts
        "fe_priority" -> R.string.field_fe_priority
        "fe_safety" -> R.string.field_fe_safety
        "fe_time_est" -> R.string.field_fe_time_est
        "fe_deadline" -> R.string.field_fe_deadline
        // Insurance Adjuster
        "ia_policy" -> R.string.field_ia_policy
        "ia_claimant" -> R.string.field_ia_claimant
        "ia_incident_type" -> R.string.field_ia_incident_type
        "ia_damage" -> R.string.field_ia_damage
        "ia_liability" -> R.string.field_ia_liability
        "ia_repair_cost" -> R.string.field_ia_repair_cost
        "ia_evidence" -> R.string.field_ia_evidence
        "ia_inspection_date" -> R.string.field_ia_inspection_date
        else -> return displayName
    }
)

@Composable
fun PersonaField.localizedDescription(): String = stringResource(
    when (id) {
        // Sales Rep
        "sr_client_name" -> R.string.field_sr_client_name_desc
        "sr_company" -> R.string.field_sr_company_desc
        "sr_deal_value" -> R.string.field_sr_deal_value_desc
        "sr_pain_points" -> R.string.field_sr_pain_points_desc
        "sr_interest_level" -> R.string.field_sr_interest_level_desc
        "sr_competitive" -> R.string.field_sr_competitive_desc
        "sr_next_action" -> R.string.field_sr_next_action_desc
        "sr_followup_date" -> R.string.field_sr_followup_date_desc
        // Field Engineer
        "fe_site_name" -> R.string.field_fe_site_name_desc
        "fe_asset_id" -> R.string.field_fe_asset_id_desc
        "fe_issue_desc" -> R.string.field_fe_issue_desc_desc
        "fe_parts" -> R.string.field_fe_parts_desc
        "fe_priority" -> R.string.field_fe_priority_desc
        "fe_safety" -> R.string.field_fe_safety_desc
        "fe_time_est" -> R.string.field_fe_time_est_desc
        "fe_deadline" -> R.string.field_fe_deadline_desc
        // Insurance Adjuster
        "ia_policy" -> R.string.field_ia_policy_desc
        "ia_claimant" -> R.string.field_ia_claimant_desc
        "ia_incident_type" -> R.string.field_ia_incident_type_desc
        "ia_damage" -> R.string.field_ia_damage_desc
        "ia_liability" -> R.string.field_ia_liability_desc
        "ia_repair_cost" -> R.string.field_ia_repair_cost_desc
        "ia_evidence" -> R.string.field_ia_evidence_desc
        "ia_inspection_date" -> R.string.field_ia_inspection_date_desc
        else -> return description
    }
)
