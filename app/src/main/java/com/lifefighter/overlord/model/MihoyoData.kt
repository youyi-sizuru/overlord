package com.lifefighter.overlord.model

/**
 * @author xzp
 * @created on 2021/3/9.
 */
class MihoyoData<T>(
    val retcode: Int? = null,
    val status: String? = null,
    val message: String? = null,
    val data: T? = null
)