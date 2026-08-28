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
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val BASE = "https://uhhfthgbtpuljelmqmpc.supabase.co"
private const val KEY = "sb_publishable_u-JbcmNjLKPVrvPz9f7W1Q_GJ4KfXy7"
private const val DEVICE = "7c7d40c8-9c8e-4b28-ae63-5cfe2b8d9101"
private val MEDIA = "application/json".toMediaType()
data class KitchenOrder(val id:String,val number:Long,val table:String,val status:String,val foods:List<String>)
class Api {
 private val http=OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build()
 private fun req(path:String)=Request.Builder().url(BASE+path).header("apikey",KEY).header("Authorization","Bearer $KEY")
 suspend fun orders():List<KitchenOrder>=withContext(Dispatchers.IO){val body="{\"p_device_token\":\"$DEVICE\"}".toRequestBody(MEDIA);http.newCall(req("/rest/v1/rpc/get_kitchen_orders").post(body).build()).execute().use{r->val text=r.body?.string().orEmpty();if(!r.isSuccessful)error("เชื่อมต่อครัวไม่ได้ (${r.code})");Json.parseToJsonElement(text).jsonArray.map{e->val x=e.jsonObject;val foods=x["foods"]?.jsonArray?.map{f->"${f.jsonObject["quantity"]!!.jsonPrimitive.int} ${f.jsonObject["name"]!!.jsonPrimitive.content}"}?:emptyList();KitchenOrder(x["id"]!!.jsonPrimitive.content,x["order_no"]!!.jsonPrimitive.long,x["table"]!!.jsonPrimitive.content,x["status"]!!.jsonPrimitive.content,foods)}}}
 suspend fun status(id:String,status:String)=withContext(Dispatchers.IO){val body="{\"p_device_token\":\"$DEVICE\",\"p_order_id\":\"$id\",\"p_status\":\"$status\"}".toRequestBody(MEDIA);http.newCall(req("/rest/v1/rpc/kitchen_set_order_status").post(body).build()).execute().use{r->if(!r.isSuccessful)error("บันทึกไม่ได้ (${r.code})")}}
}
class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener{
 private var tts:TextToSpeech?=null;private var ready=false;private var pending:String?=null;private var ttsStatus by mutableStateOf("กำลังตรวจเสียง…");private val notify=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);initTts();if(Build.VERSION.SDK_INT>=33)notify.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MaterialTheme{KitchenApp(Api(),ttsStatus){speak(it)}}}}
 private fun initTts(){ready=false;tts?.shutdown();tts=try{TextToSpeech(this,this,"com.google.android.tts")}catch(_:Exception){TextToSpeech(this,this)}}
 override fun onInit(s:Int){if(s!=TextToSpeech.SUCCESS){ttsStatus="ไม่พบเครื่องเสียง TTS";return};val e=tts?:return;var r=e.setLanguage(Locale("th","TH"));if(r<0)r=e.setLanguage(Locale("th"));ready=r>=0;ttsStatus=if(ready)"เสียงไทยพร้อม" else "ไม่มีภาษาไทย";e.setSpeechRate(.92f);pending?.takeIf{ready}?.let{e.speak(it,TextToSpeech.QUEUE_FLUSH,null,"pending")};pending=null}
 private fun speak(t:String){if(ready){val r=tts?.speak(t,TextToSpeech.QUEUE_FLUSH,null,"s-${System.nanoTime()}");ttsStatus=if(r==TextToSpeech.SUCCESS)"กำลังพูด…" else "เสียงผิดพลาด"}else{pending=t;initTts()}}
 override fun onDestroy(){tts?.shutdown();super.onDestroy()}
}
@Composable fun KitchenApp(api:Api,ttsStatus:String,speak:(String)->Unit){
 val scope=rememberCoroutineScope();var sound by remember{mutableStateOf(true)};var orders by remember{mutableStateOf(emptyList<KitchenOrder>())};var msg by remember{mutableStateOf("")};val announced=remember{mutableSetOf<String>()};val voiceQueue=remember{mutableStateListOf<String>()}
 suspend fun refresh(){try{val a=api.orders();if(sound)a.filter{it.status=="new"&&it.id !in announced&&it.foods.isNotEmpty()}.forEach{announced.add(it.id);voiceQueue.add("มีออเดอร์ใหม่ ${it.table} ${it.foods.joinToString(" ")}")};orders=a;msg=""}catch(e:Exception){msg=e.message?:"เชื่อมต่อไม่ได้"}}
 LaunchedEffect(Unit){while(true){refresh();delay(2000)}}
 LaunchedEffect(Unit){while(true){if(sound&&voiceQueue.isNotEmpty()){speak(voiceQueue.removeAt(0));delay(10000)}else delay(250)}}
 Scaffold(topBar={Surface(shadowElevation=2.dp){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("ครัว • สั่งครับ",style=MaterialTheme.typography.titleLarge);Text("${orders.size} งานค้าง • $ttsStatus")};Row{OutlinedButton(onClick={speak("ทดสอบเสียงภาษาไทย สั่งครับ ครัว")}){Text("ทดสอบเสียง")};Spacer(Modifier.width(8.dp));Button(onClick={sound=!sound}){Text(if(sound)"🔊 เปิดแล้ว" else "🔇 ปิดเสียง")}}}}}){p->Column(Modifier.padding(p)){if(msg.isNotEmpty())Text(msg,Modifier.padding(12.dp));if(orders.isEmpty())Text("ยังไม่มีออเดอร์",Modifier.padding(24.dp))else LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(orders,key={it.id}){o->Card{Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(o.table,style=MaterialTheme.typography.headlineSmall);Text("#${o.number}")};o.foods.forEach{Text(it,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=5.dp))};val next=when(o.status){"new"->"accepted";"accepted"->"preparing";"preparing"->"ready";else->"completed"};Button(onClick={scope.launch{try{api.status(o.id,next);refresh()}catch(e:Exception){msg=e.message?:"บันทึกไม่ได้"}}},modifier=Modifier.fillMaxWidth()){Text(when(o.status){"new"->"รับออเดอร์";"accepted"->"เริ่มทำ";"preparing"->"พร้อมเสิร์ฟ";else->"เสร็จแล้ว"})}}}}}}}
}
