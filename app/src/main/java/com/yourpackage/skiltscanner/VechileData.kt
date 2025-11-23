package com.yourpackage.skiltscanner

data class VehicleResponse(
    val kjoretoydataListe: List<VehicleData>?
)

data class VehicleData(
    val kjennemerke: List<Kjennemerke>?,
    val godkjenning: Godkjenning?,
    val periodiskKjoretoyKontroll: PeriodiskKjoretoyKontroll?
)

data class Kjennemerke(
    val kjennemerketype: String?,
    val kjennemerke: String?
)

data class Godkjenning(
    val tekniskGodkjenning: TekniskGodkjenning?
)

data class TekniskGodkjenning(
    val tekniskeData: TekniskeData?
)

data class TekniskeData(
    val generelt: Generelt?
)

data class Generelt(
    val merke: List<Merke>?,
    val handelsbetegnelse: List<String>?,
    val arsmodell: String?
)

data class Merke(
    val merke: String?
)

data class PeriodiskKjoretoyKontroll(
    val kontrollfrist: String?,
    val sistKontrollert: String?
)