package com.at.recallly.data.repository

import com.at.recallly.data.local.file.CustomFieldDto
import com.at.recallly.data.local.file.CustomFieldFileStorage
import com.at.recallly.domain.model.FieldType
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.repository.CustomFieldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CustomFieldRepositoryImpl(
    private val fileStorage: CustomFieldFileStorage
) : CustomFieldRepository {

    private val cache = MutableStateFlow<List<CustomFieldDto>>(emptyList())

    suspend fun loadFromDisk() {
        cache.value = fileStorage.readAll()
    }

    override fun getCustomFieldsForPersona(persona: Persona): Flow<List<PersonaField>> {
        return cache.map { dtos ->
            dtos.filter { it.persona == persona.name }.map { it.toDomain() }
        }
    }

    override fun getAllCustomFields(): Flow<List<PersonaField>> {
        return cache.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun addCustomField(field: PersonaField) {
        val dto = field.toDto()
        val updated = cache.value + dto
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    override suspend fun updateCustomField(field: PersonaField) {
        val dto = field.toDto()
        val updated = cache.value.map { if (it.id == dto.id) dto else it }
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    override suspend fun deleteCustomField(fieldId: String) {
        val updated = cache.value.filter { it.id != fieldId }
        cache.value = updated
        fileStorage.writeAll(updated)
    }

    override suspend fun deleteAllCustomFields() {
        cache.value = emptyList()
        fileStorage.writeAll(emptyList())
    }

    private fun CustomFieldDto.toDomain(): PersonaField = PersonaField(
        id = id,
        displayName = displayName,
        description = description,
        persona = try { Persona.valueOf(persona) } catch (_: Exception) { Persona.SALES_REP },
        fieldType = try { FieldType.valueOf(fieldType) } catch (_: Exception) { FieldType.TEXT }
    )

    private fun PersonaField.toDto(): CustomFieldDto = CustomFieldDto(
        id = id,
        displayName = displayName,
        description = description,
        persona = persona.name,
        fieldType = fieldType.name
    )
}
