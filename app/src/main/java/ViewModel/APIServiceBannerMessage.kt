package ViewModel

import Model.BannerMessage
import retrofit2.http.GET

interface APIServiceBannerMessage {
    @GET("encourage")
    suspend fun getBusArrivalInfo(): BannerMessage
}