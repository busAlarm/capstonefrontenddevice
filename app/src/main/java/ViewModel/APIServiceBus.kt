package ViewModel

import Model.BusData
import retrofit2.http.GET
import retrofit2.http.Query

interface APIServiceBus {
    @GET("businfo")
    suspend fun getBusArrivalInfo(@Query(value = "bus") busName: String): BusData
}