package ViewModel

import Model.BusDataGG
import Model.BusDataShuttle
import retrofit2.http.GET
import retrofit2.http.Query

interface APIServiceShuttle {
    @GET("businfo")
    suspend fun getBusArrivalInfo(@Query(value="bus") busName: String): BusDataShuttle
}