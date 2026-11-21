package com.ahmad.keyboard.ime

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast

class VoiceActivity : Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        try { startActivityForResult(i, 1) } catch (e: Exception) {
            Toast.makeText(this, "No speech recognizer available", Toast.LENGTH_SHORT).show(); finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val t = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (resultCode == RESULT_OK && t != null) KeyboardService.instance?.commit(t)
        finish()
    }
}
