package ViewModel

import Model.BusData
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface APIServiceBus {
    @GET("businfo")
    suspend fun getBusArrivalInfo(@Query(value = "bus") busName: String): BusData

    @Headers(
        "Content-Type: application/json",
        "Access-Control-Allow-Origin: *"
    )
    @POST("busData")
    suspend fun postBusArrivalInfo(@Body busData: BusData): Call<ResponseBody>
}