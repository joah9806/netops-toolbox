package com.qoder.netops

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qoder.netops.ui.NetOpsApp
import com.qoder.netops.ui.theme.NetOpsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetOpsTheme {
                NetOpsApp()
            }
        }
    }
}