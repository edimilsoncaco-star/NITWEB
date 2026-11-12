package com.nexus.painel

object NexusBridge {

    external fun parsePacket(data: ByteArray, len: Int)
    external fun getPlayerPositions(): FloatArray
    external fun runAimbot(fov: Float, sw: Float, sh: Float): FloatArray
    external fun detectGame(): Int
    external fun updateViewMatrix(matrix: FloatArray)   // novo — alimenta g_view_matrix no C++

    init {
        System.loadLibrary("nexus_esp")
    }
}
