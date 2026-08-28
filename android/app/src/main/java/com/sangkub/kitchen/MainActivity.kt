package com.sangkub.kitchen

import android.Manifest
import android.content.Context
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

private const val BASE="https://uhhfthgbtpuljelmqmpc.supabase.co"
private const val KEY="sb_publishable_u-JbcmNjLKPVrvPz9f7W1Q_GJ4KfXy7"
private val JSON_MEDIA="application/json".toMediaType()
data class KitchenOrder(val id:String,val number:Long,val table:String,val status:String,val foods:List<String>)

class Api(ctx:Context){
 private val http=OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build()
 private val prefs=ctx.getSharedPreferences("sangkub",Context.MODE_PRIVATE)
 var token:String?=prefs.getString("access",null);private set
 private fun req(url:String)=Request.Builder().url(BASE+url).header("apikey",KEY).apply{token?.let{header("Authorization","Bearer $it")}}
 suspend fun orders():List<KitchenOrder>=withContext(Dispatchers.IO){
  val url="/rest/v1/orders?select=id,order_no,status,tables(name),order_items(item_name,quantity,station)&status=in.(new,accepted,preparing,ready)&order=created_at.asc&limit=100"
  http.newCall(req(url).get().build()).execute().use{r->
   val text=r.body?.string().orEmpty();if(!r.isSuccessful)error(if(r.code==401)"เซสชันทดสอบหมดอายุ" else text)
   Json.parseToJsonElement(text).jsonArray.map{e->val x=e.jsonObject;val table=x["tables"]?.jsonObject?.get("name")?.jsonPrimitive?.content?:"โต๊ะ";val foods=x["order_items"]?.jsonArray?.filter{it.jsonObject["station"]?.jsonPrimitive?.content=="kitchen"}?.map{"${it.jsonObject["quantity"]!!.jsonPrimitive.int} ${it.jsonObject["item_name"]!!.jsonPrimitive.content}"}?:emptyList();KitchenOrder(x["id"]!!.jsonPrimitive.content,x["order_no"]!!.jsonPrimitive.long,table,x["status"]!!.jsonPrimitive.content,foods)}
  }
 }
 suspend fun status(id:String,status:String)=withContext(Dispatchers.IO){val body=buildJsonObject{put("p_order_id",id);put("p_status",status)}.toString().toRequestBody(JSON_MEDIA);http.newCall(req("/rest/v1/rpc/set_order_status").post(body).build()).execute().use{if(!it.isSuccessful)error(it.body?.string().orEmpty())}}
}

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener{
 private var tts:TextToSpeech?=null
 private var ready=false
 private var pending:String?=null
 private var ttsStatus by mutableStateOf("กำลังตรวจเสียง…")
 private val notify=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);initTts();if(Build.VERSION.SDK_INT>=33)notify.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MaterialTheme{KitchenApp(Api(this),ttsStatus){speak(it)}}}}
 private fun initTts(){ready=false;ttsStatus="กำลังตรวจเสียง…";tts?.shutdown();tts=try{TextToSpeech(this,this,"com.google.android.tts")}catch(_:Exception){TextToSpeech(this,this)}}
 override fun onInit(status:Int){if(status!=TextToSpeech.SUCCESS){ttsStatus="ไม่พบเครื่องเสียง TTS";return};val e=tts?:return;var r=e.setLanguage(Locale("th","TH"));if(r==TextToSpeech.LANG_MISSING_DATA||r==TextToSpeech.LANG_NOT_SUPPORTED)r=e.setLanguage(Locale("th"));ready=r!=TextToSpeech.LANG_MISSING_DATA&&r!=TextToSpeech.LANG_NOT_SUPPORTED;ttsStatus=if(ready)"เสียงไทยพร้อม" else "TTS ไม่มีภาษาไทย";e.setSpeechRate(.92f);e.setPitch(1f);if(ready){pending?.let{e.speak(it,TextToSpeech.QUEUE_FLUSH,null,"sangkub-test")};pending=null}}
 private fun speak(text:String){if(ready){val result=tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"sangkub-${System.nanoTime()}");ttsStatus=if(result==TextToSpeech.SUCCESS)"กำลังพูด…" else "สั่งเสียงไม่สำเร็จ"}else{pending=text;initTts()}}
 override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}

@Composable
fun KitchenApp(api:Api,ttsStatus:String,speak:(String)->Unit){
 val scope=rememberCoroutineScope();var sound by remember{mutableStateOf(true)};var orders by remember{mutableStateOf(emptyList<KitchenOrder>())};var msg by remember{mutableStateOf("")};val announced=remember{mutableSetOf<String>()}
 suspend fun refresh(){try{val latest=api.orders();if(sound)latest.filter{it.status=="new"&&it.id !in announced&&it.foods.isNotEmpty()}.forEach{announced.add(it.id);speak("มีออเดอร์ใหม่ ${it.table} ${it.foods.joinToString(" ")}")};orders=latest;msg=""}catch(e:Exception){msg=e.message?:"เชื่อมต่อไม่ได้"}}
 LaunchedEffect(Unit){while(true){refresh();delay(2000)}}
 Scaffold(topBar={Surface(shadowElevation=2.dp){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("ครัว • สั่งครับ",style=MaterialTheme.typography.titleLarge);Text("${orders.size} งานค้าง • $ttsStatus")};Row{OutlinedButton(onClick={speak("ทดสอบเสียงภาษาไทย สั่งครับ ครัว")}){Text("ทดสอบเสียง")};Spacer(Modifier.width(8.dp));Button(onClick={sound=!sound}){Text(if(sound)"🔊 เปิดแล้ว" else "🔇 ปิดเสียง")}}}}}){pad->Column(Modifier.padding(pad)){if(msg.isNotEmpty())Text(msg,Modifier.padding(12.dp));if(orders.isEmpty())Text("ยังไม่มีออเดอร์",Modifier.padding(24.dp))else LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(orders,key={it.id}){o->Card{Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(o.table,style=MaterialTheme.typography.headlineSmall);Text("#${o.number}")};o.foods.forEach{Text(it,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=5.dp))};val next=when(o.status){"new"->"accepted";"accepted"->"preparing";"preparing"->"ready";else->"completed"};Button(onClick={scope.launch{try{api.status(o.id,next);refresh()}catch(e:Exception){msg=e.message?:"บันทึกไม่ได้"}}},modifier=Modifier.fillMaxWidth()){Text(when(o.status){"new"->"รับออเดอร์";"accepted"->"เริ่มทำ";"preparing"->"พร้อมเสิร์ฟ";else->"เสร็จแล้ว"})}}}}}}}
}
