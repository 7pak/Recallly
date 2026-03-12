package com.at.recallly.domain.usecase.onboarding

import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.model.PersonaFields

class GetFieldsForPersonaUseCase {
    operator fun invoke(persona: Persona): List<PersonaField> =
        PersonaFields.getFieldsForPersona(persona)
}
