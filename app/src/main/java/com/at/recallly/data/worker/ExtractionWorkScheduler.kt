package com.at.recallly.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.at.recallly.domain.repository.ExtractionScheduler
import timber.log.Timber

class ExtractionWorkScheduler(
    private val context: Context
) : ExtractionScheduler {

    override fun enqueueExtraction(voiceNoteId: String, selectedFieldIds: Set<String>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            ExtractionWorker.KEY_VOICE_NOTE_ID to voiceNoteId,
            ExtractionWorker.KEY_SELECTED_FIELD_IDS to selectedFieldIds.toTypedArray()
        )

        val workRequest = OneTimeWorkRequestBuilder<ExtractionWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("extraction_$voiceNoteId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Timber.d("Enqueued extraction work for note: $voiceNoteId")
    }
}
