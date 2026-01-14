package org.example.project_dw.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen


// Здесь просто разные приколюхи
class ExampleScreen : Screen {
    @Composable
    override fun Content() {
        Tooltips()
    }
}

// https://www.youtube.com/watch?v=GzQ4VNjFnRY

@Composable
fun ScrollableList() {
    val verticalScroll = rememberScrollState(0)
    val horizontalScroll = rememberScrollState(0)
    Box(
        modifier = Modifier.fillMaxSize().padding(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            for (item in 0..30) {
                Text(
                    modifier = Modifier.padding(all = 12.dp),
                    text = "Itemmmmmmmmmmmm $item",
                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(verticalScroll)
        )
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp),
            adapter = rememberScrollbarAdapter(horizontalScroll)
        )
    }

}

@Composable
fun ScrollableLazyList() {
    val lazyListState = rememberLazyListState()
    val horizontalScroll = rememberScrollState(0)
    Box(
        modifier = Modifier.fillMaxSize().padding(10.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll)
                .padding(end = 12.dp, bottom = 12.dp),
            state = lazyListState
        ) {
            items(100) { number ->
                Text(
                    modifier = Modifier.padding(all = 12.dp),
                    text = "Itemmmmmmmmmmmm $number",
                )
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp),
            adapter = rememberScrollbarAdapter(horizontalScroll)
        )
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tooltips() {
    val buttons = listOf("Contact us", "About")
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.forEachIndexed { index, title ->
            TooltipArea(
                tooltip = {
                    Surface(
                        modifier = Modifier.shadow(8.dp),
                        color = Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (index == 0) "Get in touch!" else "This is our team.",
                            modifier = Modifier.padding(all = 12.dp),
                        )
                    }
                },
                delayMillis = 600,
                tooltipPlacement = TooltipPlacement.CursorPoint(
                    alignment = Alignment.BottomEnd
                )
            ) {
                Button(onClick = { }) { Text(text = title) }
            }
            if (index == 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

}

@Composable
fun KeyEventContent() {
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = text1,
            onValueChange = { text1 = it },
            modifier = Modifier.onPreviewKeyEvent {
                if (it.key == Key.Delete && it.type == KeyEventType.KeyDown) {
                    text1 = ""
                    true
                } else false
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = text2,
            onValueChange = { text2 = it },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MouseHoverContent() {
    val scrollState = rememberScrollState(0)
    Column(
        Modifier.background(Color.White).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(10) { index ->
            var hovered by remember { mutableStateOf(false) }
            val animatedColor by animateColorAsState(
                targetValue = if (hovered) Color.LightGray else Color.Transparent,
                animationSpec = tween(200)
            )
            Text(
                "Item with the number $index",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(animatedColor)
                    .padding(all = 12.dp)
                    .onPointerEvent(PointerEventType.Enter) { hovered = true }
                    .onPointerEvent(PointerEventType.Exit) { hovered = false },
                fontSize = 30.sp
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableContent() {
    var tobBoxOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    Box(
        modifier = Modifier
            .size(100.dp)
            .offset {
                IntOffset(tobBoxOffset.x.toInt(), tobBoxOffset.y.toInt())
            }
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectDragGestures(matcher = PointerMatcher.Primary) {
                    tobBoxOffset += it
                }
            }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "Draggable",
            color = Color.White
        )
    }
}

@Composable
fun ContextMenuContent() {
    var text by remember { mutableStateOf("") }
    var onBold by remember {mutableStateOf(false)}
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContextMenuDataProvider(
            items = {
                listOf(
                    ContextMenuItem(label = "Custom Action") {
                        println("Custom Action Clicked!")
                        onBold = !onBold
                    }
                )
            }
        ) {
            TextField(value = text, onValueChange = { text = it })
            Spacer(modifier = Modifier.height(12.dp))
            SelectionContainer {
                Text("Hello world!", fontWeight = if (onBold) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}