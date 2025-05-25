package com.example.objectdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.example.objectdetector.ui.AppUiScreen
import com.example.objectdetector.ui.components.Header
import com.example.objectdetector.ui.theme.AppTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val systemUiController = rememberSystemUiController()
                val primaryColor = MaterialTheme.colorScheme.primary
                val useDarkIcons = isSystemInDarkTheme()

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = primaryColor,
                        darkIcons = useDarkIcons
                    )
                }

                Scaffold(
                    topBar = { Header() },
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(
                                top = dimensionResource(R.dimen.medium)
                            )
                            .verticalScroll(rememberScrollState())

                    ) {
                        AppUiScreen(
                            modifier = Modifier
                                .widthIn(
                                    min = dimensionResource(R.dimen.min_width),
                                    max = dimensionResource(R.dimen.max_width)
                                )
                                .fillMaxSize()
                        )
                    }

                }
            }
        }
    }
}

