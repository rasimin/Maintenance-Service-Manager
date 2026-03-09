package com.example.servicemaintainreminder.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

/**
 * TextWatcher yang otomatis memformat angka dengan titik ribuan (format Rupiah).
 * Contoh: ketik 250000 → tampil 250.000
 *
 * Gunakan [getRawValue] untuk membaca nilai angka sesungguhnya tanpa titik.
 */
class CurrencyTextWatcher(private val editText: EditText) : TextWatcher {

    private var isRunning = false
    private val formatter = NumberFormat.getInstance(Locale("in", "ID"))

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isRunning || s == null) return
        isRunning = true

        // Hapus semua titik dulu, ambil angka murni
        val cleanString = s.toString().replace(".", "").replace(",", "")

        if (cleanString.isNotEmpty()) {
            try {
                val parsed = cleanString.toLong()
                val formatted = formatter.format(parsed)
                editText.setText(formatted)
                editText.setSelection(formatted.length) // posisi kursor di akhir
            } catch (e: NumberFormatException) {
                // biarkan apa adanya jika bukan angka valid
            }
        }

        isRunning = false
    }

    companion object {
        /**
         * Baca nilai Double dari EditText yang sudah diformat (hapus titik terlebih dulu).
         */
        fun getRawValue(editText: EditText): Double {
            val raw = editText.text.toString().replace(".", "").replace(",", "")
            return if (raw.isEmpty()) 0.0 else raw.toDoubleOrNull() ?: 0.0
        }

        /**
         * Attach CurrencyTextWatcher ke EditText sekaligus ganti inputType ke number.
         */
        fun attach(editText: EditText) {
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            editText.addTextChangedListener(CurrencyTextWatcher(editText))
        }
    }
}
