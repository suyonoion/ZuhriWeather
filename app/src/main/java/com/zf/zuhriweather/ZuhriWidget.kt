package com.zf.zuhriweather

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ZuhriWidget : GlanceAppWidget() {
    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(12.dp)
        ) {
            Text(
                text = "[ZF] SPASIAL: DESA BLOROK", 
                style = TextStyle(color = ColorProvider(Color.Cyan))
            )
            Text(
                text = "Termal: 31°C | Fluida: 65%", 
                style = TextStyle(color = ColorProvider(Color.White))
            )
            Spacer(modifier = GlanceModifier.padding(8.dp))
            Text(
                text = "LEDGER ZONA MERAH", 
                style = TextStyle(color = ColorProvider(Color.Red))
            )
            Row {
                Text(text = "Filipina | ", style = TextStyle(color = ColorProvider(Color.White)))
                Text(text = "7.8 Mw | ", style = TextStyle(color = ColorProvider(Color.Yellow)))
                Text(text = "Ruptur Litosfer", style = TextStyle(color = ColorProvider(Color.White)))
            }
        }
    }
}

class ZuhriWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ZuhriWidget()
}
