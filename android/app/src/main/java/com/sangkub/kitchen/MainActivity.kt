package com.sangkub.kitchen

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { MaterialTheme { KitchenApp { text -> speak(text) } } }
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) { tts.language = Locale("th","TH"); tts.setSpeechRate(.92f) } }
    private fun speak(text:String){ tts.speak(text,TextToSpeech.QUEUE_ADD,null,text.hashCode().toString()) }
    override fun onDestroy(){ tts.stop();tts.shutdown();super.onDestroy() }
}

data class KitchenOrder(val id:String,val number:Long,val table:String,val status:String,val foods:List<String>)

@Composable
fun KitchenApp(speak:(String)->Unit){
    var email by remember{mutableStateOf("")};var signed by remember{mutableStateOf(false)};var sound by remember{mutableStateOf(false)};var orders by remember{mutableStateOf(listOf<KitchenOrder>())};var message by remember{mutableStateOf("")}
    if(!signed){
        Surface(Modifier.fillMaxSize()){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.Center){Text("สั่งครับ • ครัว",style=MaterialTheme.typography.headlineLarge);Spacer(Modifier.height(10.dp));Text("แอปครัว Android Native");Spacer(Modifier.height(24.dp));OutlinedTextField(email,{email=it},label={Text("อีเมลพนักงาน")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));Button(onClick={message="ระบบ Native Auth กำลังเชื่อม Supabase"},modifier=Modifier.fillMaxWidth()){Text("เข้าสู่ระบบ")};if(message.isNotEmpty())Text(message,Modifier.padding(top=12.dp))}}
        return
    }
    Scaffold(topBar={Surface(shadowElevation=2.dp){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("ครัว • สั่งครับ",style=MaterialTheme.typography.titleLarge);Text("${orders.size} งานค้าง")};Button(onClick={sound=true;speak("เปิดเสียงครัวแล้ว")}){Text(if(sound)"🔊 เปิดแล้ว" else "🔊 เปิดเสียง")}}}}){pad->
        if(orders.isEmpty())Box(Modifier.padding(pad).fillMaxSize()){Text("ยังไม่มีออเดอร์",Modifier.padding(24.dp))} else LazyColumn(Modifier.padding(pad).padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(orders){o->Card{Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(o.table,style=MaterialTheme.typography.headlineSmall);Text("#${o.number}")};o.foods.forEach{Text(it,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=5.dp))};Button(onClick={/* RPC status wiring next */},modifier=Modifier.fillMaxWidth()){Text(when(o.status){"new"->"รับออเดอร์";"accepted"->"เริ่มทำ";"preparing"->"พร้อมเสิร์ฟ";else->"เสร็จแล้ว"})}}}}}
    }
}
