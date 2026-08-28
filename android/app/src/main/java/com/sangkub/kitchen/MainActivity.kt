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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
private val JSON_MEDIA = "application/json".toMediaType()

data class KitchenOrder(val id: String, val number: Long, val table: String, val status: String, val foods: List<String>)

class Api(ctx: Context) {
    private val http = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val prefs = ctx.getSharedPreferences("sangkub", Context.MODE_PRIVATE)
    var token: String? = prefs.getString("access", null); private set
    fun logout() { prefs.edit().clear().apply(); token = null }
    private fun req(url: String): Request.Builder = Request.Builder().url(BASE + url).header("apikey", KEY).apply { token?.let { header("Authorization", "Bearer $it") } }
    private fun saveToken(s: String) { token = Json.parseToJsonElement(s).jsonObject["access_token"]!!.jsonPrimitive.content; prefs.edit().putString("access", token).apply() }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val body = buildJsonObject { put("email", email); put("password", password) }.toString().toRequestBody(JSON_MEDIA)
        http.newCall(req("/auth/v1/token?grant_type=password").post(body).build()).execute().use { r -> val s=r.body?.string().orEmpty(); if(!r.isSuccessful) error("อีเมลหรือรหัสผ่านไม่ถูกต้อง"); saveToken(s) }
        claimRestaurant()
    } }
    suspend fun signup(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val body=buildJsonObject { put("email",email); put("password",password) }.toString().toRequestBody(JSON_MEDIA)
        http.newCall(req("/auth/v1/signup").post(body).build()).execute().use { r -> val s=r.body?.string().orEmpty(); if(!r.isSuccessful) error(runCatching { Json.parseToJsonElement(s).jsonObject["msg"]?.jsonPrimitive?.content }.getOrNull() ?: "สมัครไม่สำเร็จ"); val o=Json.parseToJsonElement(s).jsonObject; if(o["access_token"]==null) error("สมัครแล้ว กรุณายืนยันอีเมลก่อนเข้าสู่ระบบ"); saveToken(s) }
        claimRestaurant()
    } }
    private fun claimRestaurant() {
        val body=buildJsonObject { put("p_name","ร้านทดสอบ สั่งครับ") }.toString().toRequestBody(JSON_MEDIA)
        http.newCall(req("/rest/v1/rpc/claim_test_restaurant").post(body).build()).execute().use { if(!it.isSuccessful) error(it.body?.string().orEmpty()) }
    }
    suspend fun orders(): List<KitchenOrder> = withContext(Dispatchers.IO) {
        val url="/rest/v1/orders?select=id,order_no,status,tables(name),order_items(item_name,quantity,station)&status=in.(new,accepted,preparing,ready)&order=created_at.asc&limit=100"
        http.newCall(req(url).get().build()).execute().use { r -> val text=r.body?.string().orEmpty(); if(r.code==401){logout();error("SESSION")}; if(!r.isSuccessful) error(text); Json.parseToJsonElement(text).jsonArray.map { e -> val x=e.jsonObject; val table=x["tables"]?.jsonObject?.get("name")?.jsonPrimitive?.content?:"โต๊ะ"; val foods=x["order_items"]?.jsonArray?.filter{it.jsonObject["station"]?.jsonPrimitive?.content=="kitchen"}?.map{"${it.jsonObject["quantity"]!!.jsonPrimitive.int}× ${it.jsonObject["item_name"]!!.jsonPrimitive.content}"}?: emptyList(); KitchenOrder(x["id"]!!.jsonPrimitive.content,x["order_no"]!!.jsonPrimitive.long,table,x["status"]!!.jsonPrimitive.content,foods) } }
    }
    suspend fun status(id:String,status:String)=withContext(Dispatchers.IO){ val body=buildJsonObject{put("p_order_id",id);put("p_status",status)}.toString().toRequestBody(JSON_MEDIA); http.newCall(req("/rest/v1/rpc/set_order_status").post(body).build()).execute().use{if(!it.isSuccessful)error(it.body?.string().orEmpty())} }
}

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener{
    private lateinit var tts:TextToSpeech; private val notify=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);tts=TextToSpeech(this,this);if(Build.VERSION.SDK_INT>=33)notify.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MaterialTheme{KitchenApp(Api(this)){speak(it)}}}}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS){tts.language=Locale("th","TH");tts.setSpeechRate(.92f)}}
    private fun speak(text:String){tts.speak(text,TextToSpeech.QUEUE_ADD,null,System.nanoTime().toString())}
    override fun onDestroy(){tts.stop();tts.shutdown();super.onDestroy()}
}

@Composable fun KitchenApp(api:Api,speak:(String)->Unit){
    val scope=rememberCoroutineScope();var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var signed by remember{mutableStateOf(api.token!=null)};var sound by remember{mutableStateOf(false)};var orders by remember{mutableStateOf(emptyList<KitchenOrder>())};var msg by remember{mutableStateOf("")};val announced=remember{mutableSetOf<String>()}
    suspend fun refresh(){try{val latest=api.orders();if(sound)latest.filter{it.status=="new"&&it.id !in announced&&it.foods.isNotEmpty()}.forEach{announced.add(it.id);speak("${it.table} ${it.foods.joinToString(" ").replace("×","")}")};orders=latest}catch(e:Exception){if(e.message=="SESSION")signed=false else msg=e.message?:"เชื่อมต่อไม่ได้"}}
    LaunchedEffect(signed){if(signed)while(true){refresh();delay(2000)}}
    if(!signed){Surface(Modifier.fillMaxSize()){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.Center){Text("สั่งครับ • ครัว",style=MaterialTheme.typography.headlineLarge);Text("Android Native",Modifier.padding(vertical=8.dp));OutlinedTextField(email,{email=it},label={Text("อีเมลพนักงาน")},modifier=Modifier.fillMaxWidth());OutlinedTextField(pass,{pass=it},label={Text("รหัสผ่าน")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());Button(onClick={scope.launch{api.login(email.trim(),pass).onSuccess{signed=true;msg=""}.onFailure{msg=it.message?:"เข้าไม่ได้"}}},modifier=Modifier.fillMaxWidth()){Text("เข้าสู่ระบบ")};OutlinedButton(onClick={scope.launch{if(pass.length<6){msg="รหัสผ่านอย่างน้อย 6 ตัว"}else api.signup(email.trim(),pass).onSuccess{signed=true;msg=""}.onFailure{msg=it.message?:"สมัครไม่สำเร็จ"}}},modifier=Modifier.fillMaxWidth()){Text("สร้างร้านทดสอบ")};if(msg.isNotEmpty())Text(msg,Modifier.padding(top=10.dp))}};return}
    Scaffold(topBar={Surface(shadowElevation=2.dp){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("ครัว • สั่งครับ",style=MaterialTheme.typography.titleLarge);Text("${orders.size} งานค้าง")};Button(onClick={sound=true;speak("เปิดเสียงครัวแล้ว")}){Text(if(sound)"🔊 เปิดแล้ว" else "🔊 เปิดเสียง")}}}}){pad->Column(Modifier.padding(pad)){if(msg.isNotEmpty())Text(msg,Modifier.padding(12.dp));if(orders.isEmpty())Text("ยังไม่มีออเดอร์",Modifier.padding(24.dp))else LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(orders,key={it.id}){o->Card{Column(Modifier.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(o.table,style=MaterialTheme.typography.headlineSmall);Text("#${o.number}")};o.foods.forEach{Text(it,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(vertical=5.dp))};val next=when(o.status){"new"->"accepted";"accepted"->"preparing";"preparing"->"ready";else->"completed"};Button(onClick={scope.launch{try{api.status(o.id,next);refresh()}catch(e:Exception){msg=e.message?:"บันทึกไม่ได้"}}},modifier=Modifier.fillMaxWidth()){Text(when(o.status){"new"->"รับออเดอร์";"accepted"->"เริ่มทำ";"preparing"->"พร้อมเสิร์ฟ";else->"เสร็จแล้ว"})}}}}}}}
}