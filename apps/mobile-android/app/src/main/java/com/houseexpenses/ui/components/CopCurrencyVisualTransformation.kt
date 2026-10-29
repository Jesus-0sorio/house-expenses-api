package com.houseexpenses.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CopCurrencyVisualTransformation : VisualTransformation {

    private val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val decimalFormat = DecimalFormat("#,###", symbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val parsed = raw.toLongOrNull() ?: 0L
        val formattedNumber = decimalFormat.format(parsed)
        val formatted = "$ $formattedNumber"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clamped = offset.coerceAtMost(raw.length)
                val subRaw = raw.substring(0, clamped)
                val subNumber = subRaw.toLongOrNull() ?: 0L
                val subFormatted = "$ " + decimalFormat.format(subNumber)
                return subFormatted.length.coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return 0
                val subFormatted = formatted.substring(0, offset.coerceAtMost(formatted.length))
                val digitsCount = subFormatted.count { it.isDigit() }
                return digitsCount.coerceAtMost(raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
