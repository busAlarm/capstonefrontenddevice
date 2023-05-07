package ViewModel

import Model.BusDataGG
import retrofit2.http.GET
import retrofit2.http.Query

interface APIServiceGG {
    @GET("businfo")
    suspend fun getBusArrivalInfo(@Query(value="bus") busName: String): BusDataGG
}