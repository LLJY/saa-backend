package com.saa.backend

import kotlinx.serialization.Serializable

@Serializable
data class HelloWorld(val hello:String)

@Serializable
data class UserStaff(
        var uuid: String = "",
        var firstName: String = "",
        var middleName: String? = null,
        var lastName: String = "",
        var userLevel: Int = 0,
        var email: String = "",
        var passportNumber: String = "",
        var passportExpiry: Long = 0,
        var dateOfBirth: Long = 0,
        var country: String = "",
        var password: String = "",
        var contactNumber: Int = 0
)
@Serializable
data class CourseModel(
        var uuid: String,
        var title: String,
        var startDate: Long,
        var endDate: Long,
        var fees: Float,
        var learningOutcomes: String,
        var prerequisites: String,
        var learningActivities: String,
        var language: String,
        var covered: String,
        var attending: String,
        var applicationDeadline: Long
)

@Serializable
data class FellowshipModel(
        var uuid: String,
        var title: String,
        var outline: String,
        var course: CourseModel,
        var applicationDeadline: Long
)

@Serializable
data class ScholarshipModel(
        var uuid: String,
        var title: String,
        var eligibility: String,
        var benefits: String,
        var bondTime: Int,
        var outline: String
)

@Serializable
data class DiplomaModel(
        var uuid: String,
        var title: String,
        var fees: Float,
        var outline: String,
        var startDate: Long,
        var endDate: Long,
        var applicationDeadline: Long
)

fun newEmployeeDao() {

}

@Serializable
data class LoginUserRoute(var email: String, var password: String)