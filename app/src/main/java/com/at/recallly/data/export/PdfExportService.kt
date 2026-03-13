package com.at.recallly.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.at.recallly.R
import com.at.recallly.core.util.FieldLocalizer
import com.at.recallly.domain.model.Persona
import com.at.recallly.domain.model.PersonaField
import com.at.recallly.domain.model.VoiceNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PdfExportService(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595   // A4 in points
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 48f
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        private const val FOOTER_HEIGHT = 40f
    }

    // Paints
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = 22f
        color = 0xFF1E293B.toInt() // DeepSlate
    }

    private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textSize = 14f
        color = 0xFF1E293B.toInt()
    }

    private val subheadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 12f
        color = 0xFF475569.toInt() // Slate600
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textSize = 10f
        color = 0xFF334155.toInt() // Slate700
    }

    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textSize = 9f
        color = 0xFF64748B.toInt() // Slate500
    }

    private val fieldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 10f
        color = 0xFF475569.toInt()
    }

    private val fieldValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textSize = 10f
        color = 0xFF1E293B.toInt()
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF10B981.toInt() // ElectricMint
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    suspend fun generatePdf(
        voiceNotes: List<VoiceNote>,
        persona: Persona,
        selectedFieldIds: Set<String>,
        allPersonaFields: List<PersonaField>,
        displayName: String?
    ): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var currentY = 0f
        var pageNumber = 0

        fun startNewPage(): Canvas {
            currentPage?.let { document.finishPage(it) }
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(pageInfo)
            pages.add(page)
            currentPage = page
            currentY = MARGIN
            return page.canvas
        }

        fun ensureSpace(needed: Float): Canvas {
            if (canvas == null || currentY + needed > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) {
                canvas = startNewPage()
            }
            return canvas!!
        }

        fun drawWrappedText(paint: Paint, text: String, maxWidth: Float, x: Float = MARGIN): Float {
            val c = ensureSpace(paint.textSize + 4f)
            val lineHeight = paint.textSize + 4f
            var remaining = text
            while (remaining.isNotEmpty()) {
                if (currentY + lineHeight > PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT) {
                    canvas = startNewPage()
                }
                val count = paint.breakText(remaining, true, maxWidth, null)
                // Try to break at a space if we're not consuming the whole string
                val breakAt = if (count < remaining.length) {
                    val spaceIdx = remaining.lastIndexOf(' ', count)
                    if (spaceIdx > 0) spaceIdx + 1 else count
                } else count
                val line = remaining.substring(0, breakAt).trimEnd()
                canvas!!.drawText(line, x, currentY + paint.textSize, paint)
                currentY += lineHeight
                remaining = remaining.substring(breakAt).trimStart()
            }
            return currentY
        }

        // Sort notes newest first
        val sortedNotes = voiceNotes.sortedByDescending { it.createdAt }
        val selectedFields = allPersonaFields.filter { it.id in selectedFieldIds }
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        val exportDate = Instant.now().atZone(ZoneId.systemDefault()).format(dateFormatter)

        // === COVER PAGE ===
        canvas = startNewPage()

        // App title
        currentY += 60f
        val largeTitlePaint = Paint(titlePaint).apply { textSize = 28f }
        canvas!!.drawText("Recallly", MARGIN, currentY, largeTitlePaint)
        currentY += 12f

        // Accent line
        accentPaint.style = Paint.Style.STROKE
        canvas!!.drawLine(MARGIN, currentY, MARGIN + 80f, currentY, accentPaint)
        currentY += 24f

        // Export title
        canvas!!.drawText(
            context.getString(R.string.export_pdf_title),
            MARGIN, currentY, titlePaint
        )
        currentY += 28f

        // User info
        if (!displayName.isNullOrBlank()) {
            canvas!!.drawText(displayName, MARGIN, currentY, subheadingPaint)
            currentY += 18f
        }

        // Persona
        val personaLabel = context.getString(R.string.export_pdf_persona)
        val personaName = FieldLocalizer.getLocalizedPersonaName(context, persona)
        canvas!!.drawText("$personaLabel: $personaName", MARGIN, currentY, subheadingPaint)
        currentY += 18f

        // Total notes
        canvas!!.drawText(
            context.getString(R.string.export_pdf_total_notes, sortedNotes.size),
            MARGIN, currentY, subheadingPaint
        )
        currentY += 18f

        // Date range
        if (sortedNotes.isNotEmpty()) {
            val oldest = Instant.ofEpochMilli(sortedNotes.last().createdAt)
                .atZone(ZoneId.systemDefault()).format(dateFormatter)
            val newest = Instant.ofEpochMilli(sortedNotes.first().createdAt)
                .atZone(ZoneId.systemDefault()).format(dateFormatter)
            canvas!!.drawText(
                context.getString(R.string.export_pdf_date_range, oldest, newest),
                MARGIN, currentY, subheadingPaint
            )
            currentY += 28f
        }

        // Selected fields list
        if (selectedFields.isNotEmpty()) {
            currentY += 12f
            canvas!!.drawText(
                context.getString(R.string.export_pdf_selected_fields),
                MARGIN, currentY, headingPaint
            )
            currentY += 16f

            selectedFields.forEach { field ->
                val name = FieldLocalizer.getLocalizedFieldName(context, field.id)
                canvas!!.drawText("•  $name", MARGIN + 8f, currentY, bodyPaint)
                currentY += 16f
            }
        }

        // Generated by footer on cover
        currentY = PAGE_HEIGHT - MARGIN - 20f
        canvas!!.drawText(
            context.getString(R.string.export_pdf_generated, exportDate),
            MARGIN, currentY, smallPaint
        )

        // === NOTES PAGES ===
        sortedNotes.forEachIndexed { index, note ->
            val noteNumber = index + 1
            val instant = Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault())
            val dateText = instant.format(dateFormatter)
            val timeText = instant.format(timeFormatter)

            // Ensure fresh page or enough space for note header
            ensureSpace(80f)

            // Separator line between notes (not before the first on a page)
            if (currentY > MARGIN + 10f) {
                accentPaint.style = Paint.Style.STROKE
                canvas!!.drawLine(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY, accentPaint)
                currentY += 16f
            }

            // Note header: "#1 — Mar 13, 2026 at 2:30 PM"
            val noteHeader = context.getString(R.string.export_pdf_note_header, noteNumber)
            canvas!!.drawText(noteHeader, MARGIN, currentY + headingPaint.textSize, headingPaint)

            // Date/time on the right
            val dateTimeText = "$dateText  $timeText"
            val dateWidth = smallPaint.measureText(dateTimeText)
            canvas!!.drawText(
                dateTimeText,
                MARGIN + CONTENT_WIDTH - dateWidth,
                currentY + smallPaint.textSize,
                smallPaint
            )
            currentY += headingPaint.textSize + 12f

            // Persona badge
            val personaBadge = FieldLocalizer.getLocalizedPersonaName(context, note.persona)
            val badgePaint = Paint(smallPaint).apply {
                color = 0xFF10B981.toInt()
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            canvas!!.drawText(personaBadge, MARGIN, currentY + badgePaint.textSize, badgePaint)
            currentY += badgePaint.textSize + 12f

            // Transcript section
            ensureSpace(40f)
            canvas!!.drawText(
                context.getString(R.string.export_pdf_transcript),
                MARGIN, currentY + fieldLabelPaint.textSize, fieldLabelPaint
            )
            currentY += fieldLabelPaint.textSize + 8f

            // Transcript box background
            val transcriptStartY = currentY
            fillPaint.color = 0xFFF1F5F9.toInt() // Slate100
            // Draw text first to measure, then overlay box
            val savedY = currentY
            drawWrappedText(bodyPaint, note.transcript, CONTENT_WIDTH - 16f, MARGIN + 8f)
            val transcriptEndY = currentY + 4f
            // Draw box behind text (we draw box on current canvas, text already drawn)
            // Actually let's draw box first then redraw text
            // For simplicity, just add bottom padding
            currentY = transcriptEndY + 8f

            // Extracted fields
            if (note.extractionPending) {
                ensureSpace(24f)
                val pendingPaint = Paint(smallPaint).apply { color = 0xFFEA580C.toInt() }
                canvas!!.drawText(
                    context.getString(R.string.export_pdf_pending),
                    MARGIN, currentY + pendingPaint.textSize, pendingPaint
                )
                currentY += pendingPaint.textSize + 12f
            } else if (selectedFields.isNotEmpty()) {
                ensureSpace(30f)
                canvas!!.drawText(
                    context.getString(R.string.export_pdf_extracted_fields),
                    MARGIN, currentY + fieldLabelPaint.textSize, fieldLabelPaint
                )
                currentY += fieldLabelPaint.textSize + 8f

                val labelWidth = CONTENT_WIDTH * 0.35f
                val valueWidth = CONTENT_WIDTH * 0.62f

                selectedFields.forEachIndexed { fieldIdx, field ->
                    val value = note.extractedFields[field.id]
                    val displayValue = if (value.isNullOrBlank()) {
                        context.getString(R.string.export_pdf_no_value)
                    } else value

                    val fieldName = FieldLocalizer.getLocalizedFieldName(context, field.id)

                    // Calculate row height
                    val nameLines = calculateLineCount(fieldLabelPaint, fieldName, labelWidth - 8f)
                    val valueLines = calculateLineCount(fieldValuePaint, displayValue, valueWidth - 8f)
                    val rowHeight = maxOf(nameLines, valueLines) * (bodyPaint.textSize + 4f) + 8f

                    ensureSpace(rowHeight)

                    // Alternating row background
                    if (fieldIdx % 2 == 0) {
                        fillPaint.color = 0xFFF8FAFC.toInt() // Slate50
                        canvas!!.drawRect(
                            MARGIN, currentY,
                            MARGIN + CONTENT_WIDTH, currentY + rowHeight,
                            fillPaint
                        )
                    }

                    // Field name
                    val textY = currentY + 4f
                    drawTextInBounds(canvas!!, fieldLabelPaint, fieldName, MARGIN + 4f, textY, labelWidth - 8f)

                    // Field value
                    val valuePaint = if (value.isNullOrBlank()) {
                        Paint(fieldValuePaint).apply { color = 0xFF94A3B8.toInt() }
                    } else fieldValuePaint
                    drawTextInBounds(canvas!!, valuePaint, displayValue, MARGIN + labelWidth + 4f, textY, valueWidth - 8f)

                    currentY += rowHeight
                }
                currentY += 8f
            }

            // AI Tip
            if (note.additionalNotes.isNotBlank()) {
                ensureSpace(40f)
                val tipLabelPaint = Paint(fieldLabelPaint).apply { color = 0xFF10B981.toInt() }
                canvas!!.drawText(
                    context.getString(R.string.export_pdf_ai_tip),
                    MARGIN, currentY + tipLabelPaint.textSize, tipLabelPaint
                )
                currentY += tipLabelPaint.textSize + 6f

                drawWrappedText(smallPaint, note.additionalNotes, CONTENT_WIDTH - 8f, MARGIN + 4f)
                currentY += 8f
            }

            currentY += 12f
        }

        // Finish last page
        currentPage?.let { document.finishPage(it) }

        // Add page numbers by reopening — PdfDocument doesn't support re-editing
        // So we won't add page numbers in a second pass (API limitation)
        // Instead we'll note total pages was tracked

        // Write to file
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        // Clean old exports
        exportDir.listFiles()?.forEach { it.delete() }

        val timestamp = System.currentTimeMillis()
        val file = File(exportDir, "recallly_export_$timestamp.pdf")
        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        document.close()

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun calculateLineCount(paint: Paint, text: String, maxWidth: Float): Int {
        if (text.isEmpty()) return 1
        var remaining = text
        var lines = 0
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            val breakAt = if (count < remaining.length) {
                val spaceIdx = remaining.lastIndexOf(' ', count)
                if (spaceIdx > 0) spaceIdx + 1 else count
            } else count
            remaining = remaining.substring(breakAt).trimStart()
            lines++
        }
        return lines
    }

    private fun drawTextInBounds(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float
    ) {
        var y = startY + paint.textSize
        var remaining = text
        val lineHeight = paint.textSize + 4f
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            val breakAt = if (count < remaining.length) {
                val spaceIdx = remaining.lastIndexOf(' ', count)
                if (spaceIdx > 0) spaceIdx + 1 else count
            } else count
            val line = remaining.substring(0, breakAt).trimEnd()
            canvas.drawText(line, x, y, paint)
            y += lineHeight
            remaining = remaining.substring(breakAt).trimStart()
        }
    }
}
