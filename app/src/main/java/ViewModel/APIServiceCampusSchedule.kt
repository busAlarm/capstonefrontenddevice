package ViewModel

import Model.CampusSchedule
import retrofit2.http.GET

interface APIServiceCampusSchedule {
    @GET("schedule")
    suspend fun getCampusSchedule(): CampusSchedule
}