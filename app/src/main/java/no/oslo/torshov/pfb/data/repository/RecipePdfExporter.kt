package no.oslo.torshov.pfb.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import no.oslo.torshov.pfb.data.model.Recipe
import java.io.File

object RecipePdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private val CONTENT_WIDTH = (PAGE_WIDTH - 2 * MARGIN).toInt()

    fun export(context: Context, recipes: List<Recipe>): File {
        val document = PdfDocument()
        var pageNumber = 1
        var page = newPage(document, pageNumber)
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = TextPaint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val categoryPaint = TextPaint().apply { textSize = 11f; color = Color.GRAY }
        val sectionPaint = TextPaint().apply { textSize = 13f; isFakeBoldText = true; color = Color.BLACK }
        val bodyPaint = TextPaint().apply { textSize = 11f; color = Color.DKGRAY }
        val dividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        fun startNewPage() {
            document.finishPage(page)
            pageNumber++
            page = newPage(document, pageNumber)
            canvas = page.canvas
            y = MARGIN
        }

        fun drawBlock(text: String, paint: TextPaint, spaceBefore: Float = 0f, spaceAfter: Float = 6f) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, CONTENT_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .build()
            if (y + spaceBefore + layout.height > PAGE_HEIGHT - MARGIN) startNewPage()
            y += spaceBefore
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spaceAfter
        }

        for (recipe in recipes) {
            if (y > MARGIN + 50 && y + 80 > PAGE_HEIGHT - MARGIN) startNewPage()

            drawBlock(recipe.name, titlePaint, spaceBefore = 14f, spaceAfter = 2f)
            drawBlock(recipe.category, categoryPaint, spaceAfter = 10f)

            if (recipe.ingredients.isNotEmpty()) {
                drawBlock("Ingredienser", sectionPaint, spaceBefore = 6f, spaceAfter = 3f)
                recipe.ingredients.forEach { item ->
                    val prefix = if (item.startsWith("—")) "" else "• "
                    drawBlock("$prefix$item", bodyPaint, spaceAfter = 2f)
                }
            }

            if (recipe.steps.isNotEmpty()) {
                drawBlock("Fremgangsmåte", sectionPaint, spaceBefore = 10f, spaceAfter = 3f)
                recipe.steps.forEachIndexed { i, step ->
                    drawBlock("${i + 1}.  $step", bodyPaint, spaceAfter = 3f)
                }
            }

            val tips = recipe.tips.filter { it.isNotBlank() }
            if (tips.isNotEmpty()) {
                drawBlock("Tips", sectionPaint, spaceBefore = 10f, spaceAfter = 3f)
                tips.forEach { drawBlock("• $it", bodyPaint, spaceAfter = 2f) }
            }

            val mistakes = recipe.commonMistakes.filter { it.isNotBlank() }
            if (mistakes.isNotEmpty()) {
                drawBlock("Vanlige feil", sectionPaint, spaceBefore = 10f, spaceAfter = 3f)
                mistakes.forEach { drawBlock("• $it", bodyPaint, spaceAfter = 2f) }
            }

            // Divider between recipes
            y += 12f
            if (y < PAGE_HEIGHT - MARGIN) {
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
                y += 12f
            }
        }

        document.finishPage(page)

        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"))
        val file = File(context.cacheDir, "recipes_$timestamp.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun newPage(document: PdfDocument, number: Int): PdfDocument.Page =
        document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
}
