package com.beouttv.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.beouttv.app.BuildConfig
import com.beouttv.app.data.Img
import com.beouttv.app.notify.Reminders

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(title, style = T.small, color = T.c.accent2)
        Column(Modifier.fillMaxWidth().glass(GlassShape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().tvFocus().tap { onChange(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Txt(label, Modifier.weight(1f), T.body)
        val shape = RoundedCornerShape(50)
        Box(Modifier.size(46.dp, 26.dp).then(if (on) Modifier.accentFill(shape) else Modifier.glass(shape)).padding(3.dp)) {
            Box(Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart).size(20.dp).clip(shape).background(Color.White))
        }
    }
}

@Composable
fun SettingsScreen(vm: AppVM, nav: NavVM) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    var cacheMb by remember { mutableStateOf(Img.sizeBytes() / 1_048_576f) }
    var doc by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassIconButton(Ic.Back, { nav.pop() })
            Txt(S.settings, style = T.title)
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Group(S.appearance) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip(S.light, s.theme == "light") { vm.setTheme("light") } }
                        item { Chip(S.dark, s.theme == "dark") { vm.setTheme("dark") } }
                        item { Chip(S.blue, s.theme == "blue") { vm.setTheme("blue") } }
                    }
                }
            }
            item {
                Group(S.player) {
                    Txt(S.videoQuality, style = T.body)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to S.auto, 480 to "480p", 720 to "720p", 1080 to "1080p").forEach { (h, l) ->
                            item { Chip(l, s.maxHeight == h) { vm.setMaxHeight(h) } }
                        }
                    }
                    Toggle(S.resumePlayback, s.resume) { vm.setResume(it) }
                    Toggle(S.keepScreen, s.keepScreenOn) { vm.setKeepScreen(it) }
                }
            }
            item {
                Group(S.notifications) {
                    Toggle(S.notifications, s.notifications) { on ->
                        vm.setNotifications(on) { Reminders.apply(ctx, it) }
                    }
                }
            }
            item {
                Group(S.cache) {
                    Txt("%.1f MB".format(cacheMb), style = T.body, color = T.c.textDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(S.clearCache, false) { vm.clearCache(); cacheMb = 0f }
                        Chip(S.clearHistory, false) { vm.clearHistory() }
                    }
                }
            }
            item {
                Group(S.about) {
                    Row(Modifier.fillMaxWidth()) {
                        Txt(S.version, Modifier.weight(1f), T.body)
                        Txt(BuildConfig.VERSION_NAME, style = T.body, color = T.c.textDim)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(S.privacy, doc == "p") { doc = if (doc == "p") null else "p" }
                        Chip(S.terms, doc == "t") { doc = if (doc == "t") null else "t" }
                    }
                    when (doc) {
                        "p" -> Txt(S.privacyText, style = T.small, color = T.c.textDim)
                        "t" -> Txt(S.termsText, style = T.small, color = T.c.textDim)
                    }
                }
            }
        }
    }
}
