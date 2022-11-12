package com.lifefighter.overlord.net

import androidx.work.WorkerParameters
import com.google.gson.JsonIOException
import com.lifefighter.overlord.AppConst
import com.lifefighter.overlord.action.sign.MihoyoSignHelper
import com.lifefighter.overlord.action.sign.SignWork
import com.lifefighter.overlord.model.MihoyoData
import com.lifefighter.utils.JsonUtils
import com.lifefighter.utils.logDebug
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * @author xzp
 * @created on 2021/3/9.
 */
object AppInterfaceModule {
    private val MIHOYO_QUALIFIER = StringQualifier("mihoyo")
    private val AI_QUALIFIER = StringQualifier("ai")
    private val STRING_QUALIFIER = StringQualifier("string")
    val module = module {
        single(qualifier = MIHOYO_QUALIFIER) {
            Retrofit.Builder().client(
                OkHttpClient.Builder().addInterceptor(MihoyoRequestInterceptor())
                    .addInterceptor(HttpLoggingInterceptor {
                        logDebug(it)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30L, TimeUnit.SECONDS)
                    .readTimeout(30L, TimeUnit.SECONDS)
                    .writeTimeout(30L, TimeUnit.SECONDS)
                    .build()
            ).addConverterFactory(MihoyoDataConverterFactory())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(
                    GsonConverterFactory.create(JsonUtils.gson)
                ).baseUrl("https://api-takumi.mihoyo.com").build()
        }
        single {
            get<Retrofit>(MIHOYO_QUALIFIER).create(MihoyoInterface::class.java)
        }
        single {
            MihoyoSignHelper(get(), get(), get())
        }
        worker { (workerParameters: WorkerParameters) ->
            SignWork(get(), get(), workerParameters)
        }
        single(qualifier = AI_QUALIFIER) {
            Retrofit.Builder().client(get()).addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("https://www.tuling123.com").build()
        }
        single(qualifier = STRING_QUALIFIER) {
            Retrofit.Builder().client(
                OkHttpClient.Builder().addInterceptor(MihoyoRequestInterceptor())
                    .addInterceptor(HttpLoggingInterceptor {
                        logDebug(it)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(120L, TimeUnit.SECONDS)
                    .readTimeout(120L, TimeUnit.SECONDS)
                    .writeTimeout(120L, TimeUnit.SECONDS)
                    .build()
            ).baseUrl("http://localhost/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(
                    GsonConverterFactory.create(JsonUtils.gson)
                )
                .build()
        }
        single {
            get<Retrofit>(AI_QUALIFIER).create(AiInterface::class.java)
        }
        single {
            get<Retrofit>(STRING_QUALIFIER).create(StringInterface::class.java)
        }
    }
}

class MihoyoRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest = chain.request()
        val cookie = oldRequest.header("Cookie").orEmpty()
        val requestBuilder = oldRequest.newBuilder()
        val oldUserAgent = oldRequest.header(AppConst.USER_AGENT)
        val newUserAgent = if (oldUserAgent.isNullOrEmpty()) {
            "Mozilla/5.0 (Linux; Android 10; MIX 2 Build/QKQ1.190825.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.101 Mobile Safari/537.36 miHoYoBBS/2.35.2"
        } else {
            if (oldUserAgent.contains("miHoYoBBS")) {
                oldUserAgent
            } else {
                oldUserAgent.plus(" miHoYoBBS/2.35.2")
            }
        }
        requestBuilder.header(AppConst.USER_AGENT, newUserAgent)
        requestBuilder.addHeader(
            "Referer",
            "https://webstatic.mihoyo.com/bbs/event/signin-ys/index.html?bbs_auth_required=true&act_id=e202009291139501&utm_source=bbs&utm_medium=mys&utm_campaign=icon"
        )
        requestBuilder.addHeader("Accept-Encoding", "gzip, deflate, br")
        val buffer: ByteBuffer = ByteBuffer.wrap(ByteArray(16 + cookie.toByteArray().size))
        buffer.putLong(UUID_NAMESPACE.mostSignificantBits)
        buffer.putLong(UUID_NAMESPACE.leastSignificantBits)
        buffer.put(cookie.toByteArray())

        val uuidBytes: ByteArray = buffer.array()
        requestBuilder.addHeader(
            "x-rpc-device_id",
            UUID.nameUUIDFromBytes(uuidBytes).toString()
                .replace("-", "").toUpperCase(Locale.getDefault())
        )
        requestBuilder.addHeader("x-rpc-client_type", "5")
        requestBuilder.addHeader("x-rpc-app_version", "2.35.2")
        requestBuilder.addHeader("DS", createDs())
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }

    private fun createDs(): String {
        val n = "N50pqm7FSy2AkFz2B3TqtuZMJ5TOl3Ep"
        val i = System.currentTimeMillis().div(1000).toString()
        val r = (1..6)
            .map { Random.nextInt(0, charList.size) }
            .map(charList::get)
            .joinToString("")
        val md = MessageDigest.getInstance("MD5")
        val c = BigInteger(1, md.digest("salt=${n}&t=${i}&r=${r}".toByteArray())).toString(16)
            .padStart(32, '0')
        return listOf(i, r, c).joinToString(",")
    }

    companion object {
        private val charList =
            ('a'..'z').toList().plus((0..9).map { it.toString().toCharArray().first() })
        private val UUID_NAMESPACE = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
    }
}


class MihoyoDataConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return MihoyoDataConverter<Any>(type)
    }
}


class MihoyoDataConverter<T>(private val type: Type) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T? {
        return value.use {
            val data = JsonUtils.read<MihoyoData<T>>(
                it.charStream(),
                JsonUtils.newParameterizedType(MihoyoData::class.java, type)
            ) ?: throw JsonIOException("json result is null")
            if ((data.retcode == 0 || data.status == "success").not()) {
                throw MihoyoException(data.retcode ?: -1, data.message.orEmpty())
            }
            data.data
        }
    }

}

class MihoyoException(val code: Int, message: String) : Exception(message)