package com.ethernom.gattuartserver.util

import java.util.*
import kotlin.experimental.and

object Conversion {
    /* For convert integer to UUID*/
    fun convertFromInteger(i: Int): UUID? {
        val MSB = 0x0000000000001000L
        val LSB = -0x7fffff7fa064cb05L
        val value = (i and (-0x1.toLong()).toInt()).toLong()
        return UUID(MSB or (value shl 32), LSB)
    }

    fun toUnsignedIntArray(barray: ByteArray): IntArray {
        val ret = IntArray(barray.size)
        for (i in barray.indices) {
            ret[i] = (barray[i] and 0xff.toByte()).toInt() // Range 0 to 255, not -128 to 128
        }
        return ret
    }

    /*Convert hex to ASCII*/
    fun convertHexToAscII(hex: String): String? {
        if (hex.length % 2 != 0) {
            System.err.println("Invlid hex string.")
            return ""
        }
        val builder = StringBuilder()
        var i = 0
        while (i < hex.length) {

            // Step-1 Split the hex string into two character group
            val s = hex.substring(i, i + 2)
            // Step-2 Convert the each character group into integer using valueOf method
            val n = Integer.valueOf(s, 16)
            // Step-3 Cast the integer value to char
            builder.append(n.toChar())
            i = i + 2
        }
        return builder.toString()
    }

    fun trim(bytes: ByteArray): ByteArray? {
        var i = bytes.size - 1
        while (i >= 0 && bytes[i].toInt() == 0) {
            --i
        }
        return Arrays.copyOf(bytes, i + 1)
    }

    fun hexaToByteArray(str:String):ByteArray {
        val value = ByteArray(str.length / 2)
        for (i in value.indices) {
            val index = i * 2
            val j: Int = str.substring(index, index + 2).toInt(16)
            value[i] = j.toByte()
        }
        return value
    }


}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
fun ByteArray.hexa(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}

fun String.hexa(): ByteArray? {
    val str = this

    val `val` = ByteArray(str.length / 2)

    for (i in `val`.indices) {
        val index = i * 2
        val j: Int = str.substring(index, index + 2).toInt(16)
        `val`[i] = j.toByte()
    }

    return `val`
}

