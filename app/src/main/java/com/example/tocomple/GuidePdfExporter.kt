package com.example.tocomple

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun exportGuidePdf(
    context: Context,
    templateName: String,
    summary: BusinessSummary,
    plannedTypes: List<Pair<CompletoType, Int>>
): Uri {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40
    val contentWidth = pageWidth - margin * 2

    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val sectionPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 17f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 11f
        isAntiAlias = true
    }
    val boldBodyPaint = Paint(bodyPaint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = android.graphics.Color.BLACK
    }
    val subtlePaint = Paint().apply {
        color = android.graphics.Color.rgb(180, 180, 180)
        strokeWidth = 1.2f
        isAntiAlias = true
    }

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    var cursorY = margin.toFloat()

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        cursorY = margin.toFloat()
    }

    fun ensureSpace(lines: Int = 1, extra: Float = 16f) {
        val neededHeight = lines * 18f + extra
        if (cursorY + neededHeight > pageHeight - margin) {
            newPage()
        }
    }

    fun drawLine(text: String, paint: Paint = bodyPaint, extraSpacing: Float = 18f) {
        ensureSpace()
        canvas.drawText(text, margin.toFloat(), cursorY, paint)
        cursorY += extraSpacing
    }

    fun drawWrapped(text: String, paint: Paint = bodyPaint, extraSpacing: Float = 16f) {
        val words = text.split(" ")
        var currentLine = ""
        words.forEach { word ->
            val trial = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(trial) <= contentWidth) {
                currentLine = trial
            } else {
                drawLine(currentLine, paint, extraSpacing)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            drawLine(currentLine, paint, extraSpacing)
        }
    }

    fun drawSectionTitle(text: String) {
        ensureSpace(lines = 2, extra = 26f)
        cursorY += 8f
        canvas.drawLine(
            margin.toFloat(),
            cursorY,
            (pageWidth - margin).toFloat(),
            cursorY,
            subtlePaint
        )
        cursorY += 20f
        canvas.drawText(text, margin.toFloat(), cursorY, sectionPaint)
        cursorY += 24f
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM yyyy", Locale.forLanguageTag("es-CL"))
    val today = LocalDate.now().format(dateFormatter)

    drawLine("Guia de negocio", titlePaint, 32f)
    drawLine("Fecha: $today", bodyPaint)
    drawLine("Base usada: $templateName", bodyPaint)
    cursorY += 14f

    drawSectionTitle("Resumen general")
    drawLine("Total a fabricar: ${formatCompleteLabel(summary.totalProducts)}", boldBodyPaint)
    drawLine("Panes: ${formatCount(summary.breads, "pan", "panes")}")
    summary.proteins.forEach { protein ->
        drawLine("${protein.label}: ${formatCount(protein.count, protein.singular, protein.plural)}")
    }
    drawLine("Costo estimado: ${formatCurrency(summary.totalCost)}")
    drawLine("Ingreso esperado: ${formatCurrency(summary.totalRevenue)}")
    drawLine("Ganancia estimada: ${formatCurrency(summary.totalProfit)}", boldBodyPaint)

    drawSectionTitle("Detalle a fabricar")
    plannedTypes.forEach { (type, quantity) ->
        drawLine("${type.name}: ${formatCompleteLabel(quantity)}", boldBodyPaint)
        drawWrapped(type.description, extraSpacing = 15f)
        cursorY += 4f
    }

    drawSectionTitle("Costo y venta por tipo")
    summary.typeCosts.forEach { typeCost ->
        drawLine(typeCost.name, boldBodyPaint)
        drawWrapped(
            "${formatCompleteLabel(typeCost.quantity)} • Costo ${formatCurrency(typeCost.unitCost)} c/u • " +
                "Venta ${formatCurrency(typeCost.salePrice)} • Ingreso ${formatCurrency(typeCost.revenue)} • " +
                "Ganancia ${formatCurrency(typeCost.profit)}",
            extraSpacing = 15f
        )
        cursorY += 4f
    }

    drawSectionTitle("Compra y despacho")
    summary.ingredients.forEach { ingredient ->
        drawLine(
            "${ingredient.name}: ${formatWeight(ingredient.totalKg)} • ${
                formatCount(
                    ingredient.unitEstimate,
                    ingredient.unitLabelSingular,
                    ingredient.unitLabelPlural
                )
            }"
        )
    }

    document.finishPage(page)

    val directory = File(context.cacheDir, "shared").apply { mkdirs() }
    val fileName = "guia-negocio-${LocalDate.now()}.pdf"
    val file = File(directory, fileName)
    FileOutputStream(file).use { output ->
        document.writeTo(output)
    }
    document.close()

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

fun shareGuidePdf(context: Context, pdfUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir guia PDF"))
}
