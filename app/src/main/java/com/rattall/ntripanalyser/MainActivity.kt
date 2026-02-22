package com.rattall.ntripanalyser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rattall.ntripanalyser.ui.MainScreen
import com.rattall.ntripanalyser.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val vm: MainViewModel = viewModel()
                    MainScreen(viewModel = vm)
                }
            }
        }
    }
}