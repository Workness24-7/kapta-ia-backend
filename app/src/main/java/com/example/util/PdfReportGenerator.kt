package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.CompanyEntity
import com.example.util.KaptaCurrency
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

data class PdfSaleItem(
    val dateStr: String, // e.g. "28 Jul"
    val quantity: Int,
    val productName: String,
    val unitPrice: Double,
    val total: Double = quantity * unitPrice
)

data class PdfExpenseItem(
    val dateStr: String, // e.g. "28 Jul"
    val concept: String,
    val type: String, // "Administrativo" vs "Recurrente"
    val amount: Double
)

object PdfReportGenerator {

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(KaptaCurrency.locale())
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
        return format.format(amount)
    }

    fun generateFinancialReportPdf(
        context: Context,
        company: CompanyEntity,
        periodLabel: String,
        salesList: List<PdfSaleItem>,
        expensesList: List<PdfExpenseItem>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var y = 45f

        // ------------------------------------------------------------------
        // CABECERA: (Nombre de la empresa centrado)
        // ------------------------------------------------------------------
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(company.name.ifBlank { "KAPTA POS" }, 297.5f, y, paint)

        y += 22f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = android.graphics.Color.parseColor("#64748B")
        canvas.drawText("REPORTE FINANCIERO DE VENTAS Y GASTOS", 297.5f, y, paint)

        // FECHAS / PERIODO
        y += 22f
        paint.textSize = 11f
        paint.color = android.graphics.Color.parseColor("#4F46E5")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Periodo: $periodLabel", 297.5f, y, paint)

        y += 25f
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, y, 555f, y, paint)

        // ------------------------------------------------------------------
        // SECCIÓN 1: VENTAS
        // ------------------------------------------------------------------
        y += 28f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawText("1. Ventas", 40f, y, paint)

        y += 20f
        // Encabezado de columnas: Fecha | Cantidad | Producto | Precio (C/U) | Total
        paint.textSize = 9.5f
        paint.color = android.graphics.Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Fecha", 40f, y, paint)
        canvas.drawText("Cantidad", 110f, y, paint)
        canvas.drawText("Producto", 170f, y, paint)
        canvas.drawText("Precio (C/U)", 360f, y, paint)
        canvas.drawText("Total", 470f, y, paint)

        y += 8f
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)

        var totalVentasSum = 0.0
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = android.graphics.Color.parseColor("#1E293B")

        if (salesList.isEmpty()) {
            y += 18f
            canvas.drawText("Sin ventas en el periodo seleccionado", 40f, y, paint)
        } else {
            salesList.forEach { sale ->
                y += 18f
                val rowTotal = sale.quantity * sale.unitPrice
                totalVentasSum += rowTotal

                canvas.drawText(sale.dateStr, 40f, y, paint)
                canvas.drawText("${sale.quantity}", 110f, y, paint)
                val prodTrunc = if (sale.productName.length > 25) sale.productName.take(23) + ".." else sale.productName
                canvas.drawText(prodTrunc, 170f, y, paint)
                canvas.drawText(formatCurrency(sale.unitPrice), 360f, y, paint)
                canvas.drawText(formatCurrency(rowTotal), 470f, y, paint)
            }
        }

        y += 22f
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        canvas.drawLine(40f, y, 555f, y, paint)

        // ------------------------------------------------------------------
        // SECCIÓN 2: GASTOS
        // ------------------------------------------------------------------
        y += 28f
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawText("2. Gastos", 40f, y, paint)

        y += 20f
        // Encabezado de columnas: Fecha | Gasto | Tipo de Gastos | Valor
        paint.textSize = 9.5f
        paint.color = android.graphics.Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Fecha", 40f, y, paint)
        canvas.drawText("Gasto", 120f, y, paint)
        canvas.drawText("Tipo de Gastos", 330f, y, paint)
        canvas.drawText("Valor", 470f, y, paint)

        y += 8f
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        canvas.drawLine(40f, y, 555f, y, paint)

        var totalGastosSum = 0.0
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = android.graphics.Color.parseColor("#1E293B")

        if (expensesList.isEmpty()) {
            y += 18f
            canvas.drawText("Sin gastos en el periodo seleccionado", 40f, y, paint)
        } else {
            expensesList.forEach { exp ->
                y += 18f
                totalGastosSum += exp.amount

                val strictType = if (exp.type.contains("Admin", ignoreCase = true)) "Administrativo" else "Recurrente"

                canvas.drawText(exp.dateStr, 40f, y, paint)
                val conceptTrunc = if (exp.concept.length > 28) exp.concept.take(26) + ".." else exp.concept
                canvas.drawText(conceptTrunc, 120f, y, paint)
                canvas.drawText(strictType, 330f, y, paint)
                canvas.drawText(formatCurrency(exp.amount), 470f, y, paint)
            }
        }

        y += 22f
        paint.color = android.graphics.Color.parseColor("#CBD5E1")
        canvas.drawLine(40f, y, 555f, y, paint)

        // ------------------------------------------------------------------
        // SECCIÓN 3: TOTALES (CÁLCULO DE UTILIDAD)
        // ------------------------------------------------------------------
        y += 28f
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#0F172A")
        canvas.drawText("3. Totales (Cálculo de Utilidad)", 40f, y, paint)

        val netProfit = totalVentasSum - totalGastosSum

        y += 22f
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Ventas:", 60f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#10B981")
        canvas.drawText(formatCurrency(totalVentasSum), 160f, y, paint)

        y += 20f
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Gastos:", 60f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.parseColor("#EF4444")
        canvas.drawText(formatCurrency(totalGastosSum), 160f, y, paint)

        y += 22f
        paint.color = android.graphics.Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Neto:", 60f, y, paint)
        paint.color = if (netProfit >= 0) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.parseColor("#EF4444")
        paint.textSize = 14f
        canvas.drawText(formatCurrency(netProfit), 160f, y, paint)

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "Reporte_Financiero_${company.code}.pdf")
        val fos = FileOutputStream(outputFile)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()

        return outputFile
    }

    fun openOrSharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte PDF"))
            } catch (e2: Exception) {
                Toast.makeText(context, "Reporte PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
