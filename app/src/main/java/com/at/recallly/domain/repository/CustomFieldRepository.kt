package com.at.recallly.domain.repository

import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import kotlinx.coroutines.flow.Flow

interface CustomFieldRepository {
    fun getCustomFieldsForPersona(persona: Persona): Flow<List<PersonaField>>
    fun getAllCustomFields(): Flow<List<PersonaField>>
    suspend fun addCustomField(field: PersonaField)
    suspend fun updateCustomField(field: PersonaField)
    suspend fun deleteCustomField(fieldId: String)
    suspend fun deleteAllCustomFields()
}
