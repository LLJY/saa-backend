package com.saa.backend

import kotlinx.serialization.Serializable

@Serializable
data class HelloWorld(val hello: String)

@Serializable
data class UserParticipant(
        var uuid: String,
        var firstName: String,
        var lastName: String,
        var dob: Long,
        var email: String,
        var country: String,
        var passportNumber: String,
        var passportExpiry: Long,
        var organisation: String,
        var jobTitle: String,
        var password: String,
        var contactNumber: Int
)

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
data class CourseApplyModel(
        var course: CourseModel,
        var userUUID: String
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
data class FellowshipApplyModel(
        var fellowship: FellowshipModel,
        var userUUID: String
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
data class ScholarshipApplyModel(
        var scholarship: ScholarshipModel,
        var userUUID: String
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

@Serializable
data class DiplomaApplyModel(
        var diploma: DiplomaModel,
        var userUUID: String
)

@Serializable
data class UserApplicationModel(
        var fullName: String,
        var progressType: Int, // 0, rejected, 1 not approved, 2 in progress, 3 completed
        var applicationUUID: String,
        var user: UserParticipant,
        var courseApplicationIndex: Int // 0 = course, 1= fellowship, 2=scholarship, 3=diploma
)

@Serializable
data class ParticipantModel(var uuid: String, var fullName: String, var dob: Long, var contactNumber: String, var country: String, var passportNumber: String, var passportExpiry: Long, var organisation: String, var jobTitle: String, var email: String)

@Serializable
data class EmployeeModel(var uuid: String, var fullName: String, var dob: Long, var contactNumber: String, var country: String, var passportNumber: String, var passportExpiry: Long, var email: String, var userType: String, var approvalStatus: Int)

@Serializable
data class LoginUserRoute(var email: String, var password: String)

@Serializable
data class CourseApplicationModel(
        var course: CourseModel,
        var progressType: Int
)

@Serializable
data class FellowshipApplicationModel(
        var fellowship: FellowshipModel,
        var progressType: Int
)

@Serializable
data class DiplomaApplicationModel(
        var diploma: DiplomaModel,
        var progressType: Int
)

@Serializable
data class ScholarshipApplicationModel(
        var scholarship: ScholarshipModel,
        var progressType: Int
)
