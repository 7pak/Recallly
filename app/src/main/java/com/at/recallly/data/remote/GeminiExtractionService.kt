package com.at.recallly.data.remote

import com.at.recallly.core.result.Result
import com.at.recallly.domain.model.ExtractionResult
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.ExtractionService
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

class GeminiExtractionService(
    private val generativeModel: GenerativeModel
) : ExtractionService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractFields(
        transcript: String,
        persona: Persona,
        fields: List<PersonaField>
    ): Result<ExtractionResult> {
        return try {
            val prompt = buildPrompt(transcript, persona, fields)
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return Result.Error(Exception("Empty response from AI"))
            parseResponse(text, fields)
        } catch (e: Exception) {
            Timber.e(e, "Gemini extraction failed")
            Result.Error(e)
        }
    }

    private fun buildPrompt(
        transcript: String,
        persona: Persona,
        fields: List<PersonaField>
    ): String {
        val fieldExamples = getFieldExamples(persona)
        val fieldTable = fields.joinToString("\n") { field ->
            val example = fieldExamples[field.id] ?: ""
            "| ${field.id} | ${field.displayName} | ${field.description} | \"$example\" |"
        }
        val example = getPersonaExample(persona)

        return """
You are an expert data extraction AI for a ${persona.displayName}.
Your job is to carefully read a voice transcript and extract specific structured fields.

FIELDS TO EXTRACT (use the EXACT field IDs as JSON keys):
| Field ID | Field Name | What to look for | Example value |
|----------|------------|-------------------|---------------|
$fieldTable

CRITICAL RULES:
1. Use the EXACT field IDs listed above as JSON keys — do NOT rename or modify them
2. Match data to the CORRECT field:
   - A person's name ALWAYS goes in the name/client/claimant field, NEVER in the company/site field
   - A company or organization name ALWAYS goes in the company/site field, NEVER in the name field
   - Dollar amounts go in value/cost fields, dates go in date fields
3. Use "" (empty string) for fields NOT mentioned in the transcript
4. Do NOT invent or hallucinate data — only extract what is clearly stated or strongly implied
5. Combine related mentions — if info about the same field appears in multiple places, merge it
6. For subjective fields (Interest Level, Priority Level), infer from tone and context:
   - "really excited" / "eager" / "wants to move fast" → "Hot"
   - "interested but cautious" / "wants to think about it" → "Warm"
   - "not sure" / "just exploring" → "Cold"
7. Convert spoken numbers to written form: "fifty thousand" → "$50,000", "two weeks" → "2 weeks"
8. Keep values concise but complete — include titles, qualifiers, and context when stated

EXAMPLE:
Transcript: "${example.transcript}"
Result: ${example.result}

OUTPUT FORMAT — Return ONLY valid JSON, no markdown, no code fences, no explanation:
{"fields": {"field_id": "value", ...}, "additional_notes": "one sentence of feedback"}

For "additional_notes": Provide 1 friendly sentence about the recording quality:
- If many fields are empty → suggest what to mention next time (e.g. "Try including the client name and deal value next time")
- If all fields filled → praise (e.g. "Excellent note — all key details captured!")
- If transcript is short/unclear → note it (e.g. "Recording was brief — try adding more details for better results")

TRANSCRIPT:
\"\"\"$transcript\"\"\"
        """.trimIndent()
    }

    private fun getFieldExamples(persona: Persona): Map<String, String> = when (persona) {
        Persona.SALES_REP -> mapOf(
            "sr_client_name" to "Sarah Johnson, CTO",
            "sr_company" to "TechVenture Inc",
            "sr_deal_value" to "\$50,000",
            "sr_pain_points" to "Current system too slow, manual data entry",
            "sr_interest_level" to "Hot",
            "sr_competitive" to "Currently using Salesforce",
            "sr_next_action" to "Send proposal and schedule demo",
            "sr_followup_date" to "Next Tuesday"
        )
        Persona.FIELD_ENGINEER -> mapOf(
            "fe_site_name" to "Riverside Power Plant, Building C",
            "fe_asset_id" to "GEN-4402",
            "fe_issue_desc" to "Bearing failure on main turbine, excessive vibration",
            "fe_parts" to "SKF 6205 bearing, seal kit",
            "fe_priority" to "High",
            "fe_safety" to "High voltage area, lockout required",
            "fe_time_est" to "4 hours",
            "fe_deadline" to "End of week"
        )
        Persona.INSURANCE_ADJUSTER -> mapOf(
            "ia_policy" to "HO-2024-88431",
            "ia_claimant" to "Robert Chen",
            "ia_incident_type" to "Water damage / Flooding",
            "ia_damage" to "Basement flooded, drywall and carpet destroyed",
            "ia_liability" to "No third-party fault, natural flooding",
            "ia_repair_cost" to "\$12,000",
            "ia_evidence" to "Photos of basement, water line marks on walls",
            "ia_inspection_date" to "March 10, 2026"
        )
    }

    private data class PromptExample(val transcript: String, val result: String)

    private fun getPersonaExample(persona: Persona): PromptExample = when (persona) {
        Persona.SALES_REP -> PromptExample(
            transcript = "Just finished a meeting with Sarah Johnson from TechVenture Inc. She's their CTO. They're looking at about a fifty thousand dollar package for their team. Main issue is their current system is way too slow and they're doing everything manually. She seemed really excited about our solution. They're currently on Salesforce but not happy with it. We agreed I'll send over a proposal and set up a demo for next Tuesday.",
            result = """{"fields": {"sr_client_name": "Sarah Johnson, CTO", "sr_company": "TechVenture Inc", "sr_deal_value": "${'$'}50,000", "sr_pain_points": "Current system too slow, manual processes", "sr_interest_level": "Hot", "sr_competitive": "Currently using Salesforce, unhappy with it", "sr_next_action": "Send proposal and schedule demo", "sr_followup_date": "Next Tuesday"}, "additional_notes": "Excellent note — all key details captured!"}"""
        )
        Persona.FIELD_ENGINEER -> PromptExample(
            transcript = "On site at Riverside Power Plant Building C. The main turbine generator GEN-4402 has a bearing failure. There's excessive vibration and noise from the drive end. Going to need a new SKF 6205 bearing and a seal kit. This is high priority because the unit needs to be back online by end of week. Note that this is a high voltage area so lockout tagout is required. I estimate about four hours for the repair.",
            result = """{"fields": {"fe_site_name": "Riverside Power Plant, Building C", "fe_asset_id": "GEN-4402", "fe_issue_desc": "Bearing failure on main turbine, excessive vibration and noise from drive end", "fe_parts": "SKF 6205 bearing, seal kit", "fe_priority": "High", "fe_safety": "High voltage area, lockout/tagout required", "fe_time_est": "4 hours", "fe_deadline": "End of week"}, "additional_notes": "Great report — all fields clearly covered with good technical detail."}"""
        )
        Persona.INSURANCE_ADJUSTER -> PromptExample(
            transcript = "Inspecting the property of Robert Chen today March tenth. Policy number HO-2024-88431. This is a water damage claim from last week's flooding. The entire basement is flooded about two feet of standing water. Drywall is destroyed up to three feet and all the carpet needs to be replaced. No third party fault here it's natural flooding from the heavy rains. I'm estimating about twelve thousand dollars in repairs. Took photos of the basement and documented the water line marks on the walls.",
            result = """{"fields": {"ia_policy": "HO-2024-88431", "ia_claimant": "Robert Chen", "ia_incident_type": "Water damage / Flooding", "ia_damage": "Basement flooded (2 feet standing water), drywall destroyed up to 3 feet, carpet needs full replacement", "ia_liability": "No third-party fault, natural flooding from heavy rains", "ia_repair_cost": "${'$'}12,000", "ia_evidence": "Photos of basement, water line marks on walls documented", "ia_inspection_date": "March 10"}, "additional_notes": "Thorough inspection notes — all key claim details captured clearly."}"""
        )
    }

    private fun parseResponse(
        responseText: String,
        fields: List<PersonaField>
    ): Result<ExtractionResult> {
        return try {
            // Strip markdown code fences if present
            val cleaned = responseText
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()

            val jsonObject = json.parseToJsonElement(cleaned).jsonObject
            val fieldsObj = jsonObject["fields"]?.jsonObject ?: JsonObject(emptyMap())
            val additionalNotes = jsonObject["additional_notes"]?.jsonPrimitive?.content ?: ""

            val extractedFields = mutableMapOf<String, String>()
            for (field in fields) {
                extractedFields[field.id] = fieldsObj[field.id]?.jsonPrimitive?.content ?: ""
            }

            Result.Success(
                ExtractionResult(
                    fields = extractedFields,
                    additionalNotes = additionalNotes
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Gemini response: $responseText")
            Result.Error(Exception("Failed to parse AI response. Please try again."))
        }
    }
}
