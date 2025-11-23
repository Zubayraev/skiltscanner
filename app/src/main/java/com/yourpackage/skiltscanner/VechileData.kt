package com.yourpackage.skiltscanner

import com.google.gson.annotations.SerializedName

data class VehicleResponse(
    @SerializedName("kjoretoydataListe")
    val kjoretoydataListe: List<VehicleData>?
)

data class VehicleData(
    @SerializedName("kjennemerke")
    val kjennemerke: List<Kjennemerke>?,
    @SerializedName("godkjenning")
    val godkjenning: Godkjenning?,
    @SerializedName("periodiskKjoretoyKontroll")
    val periodiskKjoretoyKontroll: PeriodiskKjoretoyKontroll?
)

data class Kjennemerke(
    @SerializedName("kjennemerketype")
    val kjennemerketype: Kjennemerketype?,
    @SerializedName("kjennemerke")
    val kjennemerke: String?
)

data class Kjennemerketype(
    @SerializedName("kodeverdi")
    val kodeverdi: String?,
    @SerializedName("kodeNavn")
    val kodeNavn: String?
)

data class Godkjenning(
    @SerializedName("tekniskGodkjenning")
    val tekniskGodkjenning: TekniskGodkjenning?
)

data class TekniskGodkjenning(
    @SerializedName("tekniskeData")
    val tekniskeData: TekniskeData?
)

data class TekniskeData(
    @SerializedName("generelt")
    val generelt: Generelt?
)

data class Generelt(
    @SerializedName("merke")
    val merke: List<Merke>?,
    @SerializedName("handelsbetegnelse")
    val handelsbetegnelse: List<String>?,
    @SerializedName("aarsmodell")
    val aarsmodell: String?
)

data class Merke(
    @SerializedName("merke")
    val merke: String?
)

data class PeriodiskKjoretoyKontroll(
    @SerializedName("kontrollfrist")
    val kontrollfrist: String?,
    @SerializedName("sistKontrollert")
    val sistKontrollert: String?
)