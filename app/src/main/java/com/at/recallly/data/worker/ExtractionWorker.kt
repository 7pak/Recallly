package com.at.recallly.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.at.recallly.data.repository.VoiceNoteRepositoryImpl
import com.at.recallly.domain.model.PersonaFields
import com.at.recallly.domain.repository.ExtractionService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import com.at.recallly.core.result.Result as AppResult

class ExtractionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: VoiceNoteRepositoryImpl by inject()
    private val extractionService: ExtractionService by inject()

    override suspend fun doWork(): Result {
        val voiceNoteId = inputData.getString(KEY_VOICE_NOTE_ID)
            ?: return Result.failure()
        val fieldIdArray = inputData.getStringArray(KEY_SELECTED_FIELD_IDS)
            ?: return Result.failure()
        val selectedFieldIds = fieldIdArray.toSet()

        Timber.d("ExtractionWorker starting for note: $voiceNoteId")

        val voiceNote = repository.getVoiceNoteById(voiceNoteId)
        if (voiceNote == null) {
            Timber.w("Voice note $voiceNoteId not found — may have been deleted")
            return Result.failure()
        }

        val allFields = PersonaFields.getFieldsForPersona(voiceNote.persona)
        val fields = if (selectedFieldIds.isEmpty()) allFields
        else allFields.filter { it.id in selectedFieldIds }

        return when (val result = extractionService.extractFields(
            voiceNote.transcript, voiceNote.persona, fields
        )) {
            is AppResult.Success -> {
                val updated = voiceNote.copy(
                    extractedFields = result.data.fields,
                    additionalNotes = result.data.additionalNotes,
                    extractionPending = false
                )
                repository.updateVoiceNote(updated)
                Timber.d("ExtractionWorker completed for note: $voiceNoteId")
                Result.success()
            }
            is AppResult.Error -> {
                Timber.e(result.exception, "ExtractionWorker failed for note: $voiceNoteId")
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_VOICE_NOTE_ID = "voice_note_id"
        const val KEY_SELECTED_FIELD_IDS = "selected_field_ids"
    }
}
