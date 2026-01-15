package com.example.demo_musicsound.data

val DEFAULT_PADS = listOf(
    // Bank A
    PadDefault(slotId = "A0", soundKey = "kick",  label = "KICK"),
    PadDefault(slotId = "A1", soundKey = "snare", label = "SNARE"),
    PadDefault(slotId = "A2", soundKey = "hat",   label = "HAT"),
    PadDefault(slotId = "A3", soundKey = "clap",  label = "CLAP"),
    PadDefault(slotId = "A4", soundKey = "tom1",  label = "TOM1"),
    PadDefault(slotId = "A5", soundKey = "tom2",  label = "TOM2"),

    // Bank B
    PadDefault(slotId = "B0", soundKey = "rim",    label = "RIM"),
    PadDefault(slotId = "B1", soundKey = "shaker", label = "SHAK"),
    PadDefault(slotId = "B2", soundKey = "ohat",   label = "OHAT"),
    PadDefault(slotId = "B3", soundKey = "ride",   label = "RIDE"),
    PadDefault(slotId = "B4", soundKey = "fx1",    label = "FX1"),
    PadDefault(slotId = "B5", soundKey = "fx2",    label = "FX2"),
)

data class PadDefault(
    val slotId: String,
    val soundKey: String,
    val label: String
)