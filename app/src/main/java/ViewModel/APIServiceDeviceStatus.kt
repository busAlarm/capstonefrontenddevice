package ViewModel

import Model.DeviceStatus
import retrofit2.http.POST

interface APIServiceDeviceStatus {
    @POST("status")
    suspend fun postDeviceStatus(deviceStatus: DeviceStatus): DeviceStatus
}