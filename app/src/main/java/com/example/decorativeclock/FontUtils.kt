package com.example.decorativeclock
import com.example.decorativeclock.R

val customFontResourceMap = mapOf(
    "pressstart_regular" to R.font.pressstart_regular,
    "abrilfatface" to R.font.abrilfatface,
    "audiowide" to R.font.audiowide,
    "bebasneue" to R.font.bebas_neue,
    "bigshouldersdisplay" to R.font.bigshouldersdisplay,
    "gajraj_one" to R.font.gajraj_one,
    "gruppo" to R.font.gruppo,
    "lilita_one" to R.font.lilita_one,
    "poiret_one" to R.font.poiret_one,
    "roboto" to R.font.roboto,
    "tiltprism" to R.font.tiltprism
)

fun getFontResourceId(fontIdentifier: String?): Int? {
    return customFontResourceMap[fontIdentifier]
}