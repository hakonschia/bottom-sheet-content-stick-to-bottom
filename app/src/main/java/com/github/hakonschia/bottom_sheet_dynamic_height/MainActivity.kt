package com.github.hakonschia.bottom_sheet_dynamic_height

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BottomsheetdynamicheightTheme {
                var stickToBottom by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxSize()) {
                    Switch(checked = stickToBottom, onCheckedChange = { stickToBottom = !stickToBottom })
                    Text(text = "Showing " + if (stickToBottom) "stickying to bottom bottom sheet" else "default bottom sheet")

                    if (stickToBottom) {
                        StickToBottomBottomSheet()
                    } else {
                        DefaultBottomSheet()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DefaultBottomSheet() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        val state = androidx.compose.material.rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded)

        ModalBottomSheetLayout(
            sheetState = state,
            sheetBackgroundColor = Color.Red,
            sheetContent = {
                SheetContent()
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun StickToBottomBottomSheet() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        val state = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)

        ModalBottomSheetLayout2(
            sheetState = state,
            sheetBackgroundColor = Color.Red,
            sheetContent = {
                SheetContent()
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

@Composable
private fun SheetContent() {
    var maxHeight by remember { mutableStateOf(true) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (maxHeight) 350.dp else 50.dp)
    ) {
        Button(onClick = { maxHeight = !maxHeight }) {
            Text(text = "Expand")
        }
    }
}
