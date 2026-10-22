package com.bsit1c.numbersystemconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The default factory of ComponentActivity supplies the SavedStateHandle.
        val viewModel = ViewModelProvider(this)[ConverterViewModel::class.java]
        setContent {
            AppTheme {
                ConverterScreen(viewModel)
            }
        }
    }
}
