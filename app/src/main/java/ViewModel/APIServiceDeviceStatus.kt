package ViewModel

import Model.DeviceStatus
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APIServiceDeviceStatus {
    @Headers(
        "Content-Type: application/json",
        "Access-Control-Allow-Origin: *"
    )
    @POST("deviceBusData")
    suspend fun postDeviceStatus(@Body deviceStatus: DeviceStatus): Call<ResponseBody>
}