package ru.rufp.weaser.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RainViewerMaps(
    val version: String = "",
    val generated: Long = 0,
    val host: String = "",
    val radar: RainViewerFrameSet = RainViewerFrameSet()
)

@Serializable
data class RainViewerFrameSet(
    val past: List<RainViewerFrame> = emptyList(),
    val nowcast: List<RainViewerFrame> = emptyList()
)

@Serializable
data class RainViewerFrame(
    val time: Long = 0,
    val path: String = ""
)
