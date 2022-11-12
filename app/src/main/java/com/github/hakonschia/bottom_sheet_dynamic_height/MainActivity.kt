package com.github.hakonschia.bottom_sheet_dynamic_height

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.hakonschia.bottom_sheet_dynamic_height.bottomsheet.ModalBottomSheetLayout2
import com.github.hakonschia.bottom_sheet_dynamic_height.bottomsheet.rememberModalBottomSheetState
import com.github.hakonschia.bottom_sheet_dynamic_height.ui.theme.BottomsheetdynamicheightTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BottomsheetdynamicheightTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)

                    ModalBottomSheetLayout2(
                        sheetState = state,
                        sheetBackgroundColor = Color.Red,
                        sheetContent = {
                            var maxHeight by remember { mutableStateOf(true) }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (maxHeight) 500.dp else 50.dp)
                            ) {
                                Button(onClick = { maxHeight = !maxHeight }) {
                                    Text(text = "Expand")
                                }
                            }
                        }
                    ) {
                        val scope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                scope.launch {
                                    state.show()
                                }
                            }
                        ) {
                            Text(text = "Show")
                        }
                    }
                }
            }
        }
    }
}
