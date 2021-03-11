package com.lifefighter.overlord.net

import com.google.gson.JsonIOException
import com.lifefighter.overlord.model.MihoyoData
import com.lifefighter.utils.JsonUtils
import com.lifefighter.utils.logDebug
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

/**
 * @author xzp
 * @created on 2021/3/9.
 */
object AppInterfaceModule {
    private val MIHOYO_QUALIFIER = StringQualifier("mihoyo")
    val module = module {
        single(qualifier = MIHOYO_QUALIFIER) {
            Retrofit.Builder().client(
                OkHttpClient.Builder().addInterceptor(MihoyoRequestInterceptor())
                    .addInterceptor(HttpLoggingInterceptor {
                        logDebug(it)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            ).addConverterFactory(MihoyoDataConverterFactory()).addConverterFactory(
                GsonConverterFactory.create(JsonUtils.gson)
            ).baseUrl("https://api-takumi.mihoyo.com").build()
        }
        single {
            get<Retrofit>(MIHOYO_QUALIFIER).create(MihoyoInterface::class.java)
        }
    }
}

class MihoyoRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest = chain.request()
        val cookie = oldRequest.header("Cookie").orEmpty()
        val requestBuilder = oldRequest.newBuilder()
        requestBuilder.addHeader(
            "User-Agent",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) miHoYoBBS/2.3.0"
        )
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
        requestBuilder.addHeader("x-rpc-app_version", "2.3.0")
        requestBuilder.addHeader("DS", createDs())
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }

    private fun createDs(): String {
        val n = "h8w582wxwgqvahcdkpvdhbh2w9casgfl"
        val i = System.currentTimeMillis().div(1000).toString()
        val r = (1..6)
            .map { kotlin.random.Random.nextInt(0, charList.size) }
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
        private fun toBytes(uuid: UUID): ByteArray {
            // inverted logic of fromBytes()
            val out = ByteArray(16)
            val msb = uuid.mostSignificantBits
            val lsb = uuid.leastSignificantBits
            for (i in 0..7)
                out[i] = (msb shr (7 - i) * 8 and 0xff).toByte()
            for (i in 8..15)
                out[i] = (lsb shr (15 - i) * 8 and 0xff).toByte()
            return out
        }
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
            if (data.retcode != 0) {
                throw MihoyoException(data.retcode ?: -1, data.message.orEmpty())
            }
            data.data
        }
    }

}

class MihoyoException(val code: Int, message: String) : Exception(message)