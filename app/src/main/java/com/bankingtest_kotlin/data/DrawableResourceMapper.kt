package com.bankingtest_kotlin.data

import android.content.res.Resources

interface DrawableResourceMapper {
    fun resolveDrawableId(name: String): Int?
}

class AndroidDrawableResourceMapper(
    private val resources: Resources,
    private val packageName: String
) : DrawableResourceMapper {
    override fun resolveDrawableId(name: String): Int? {
        return resources.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }
    }
}
