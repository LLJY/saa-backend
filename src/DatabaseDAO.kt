package com.saa.backend

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


fun getUnixTime(): Long{
    return System.currentTimeMillis()
}

object Persons: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val lastName = varchar("last_name", 255)
    val email = varchar("email", 255)
    val dateOfBirth = long("dob_timestamp")
    val passwordHash = varchar("password_hash", 255)
    val passportNumber = varchar("passport_number", 255)
    val contactNumber = varchar("contact_number", 25)
    val passportExpiry = long("passport_expiry")
    val country = varchar("country",64)
    val createdOn = long("date_created_timestamp").clientDefault { getUnixTime() }
    val notificationToken = varchar("notification_token", 255)
}
class PersonDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PersonDao>(Persons)

    var uuid by Persons.uuid
    var firstName by Persons.firstName
    var middleName by Persons.middleName
    var lastName by Persons.lastName
    var email by Persons.email
    var dateOfBirth by Persons.dateOfBirth
    var passwordHash by Persons.passwordHash
    var passportNumber by Persons.passportNumber
    var contactNumber by Persons.contactNumber
    var passportExpiry by Persons.passportExpiry
    var country by Persons.country
    var createdOn by Persons.createdOn
    var notificationToken by Persons.notificationToken
    val employees by EmployeeDao referrersOn Employees.userInfo
    val participants by ParticipantDao referrersOn Participants.userInfo
}
object Courses: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val learningOutcomes = text("learning_outcome")
    val whoShouldAttend = text("who_should_attend")
    val prerequisites = text("prerequisites")
    val learningActivities = text("learning_activities")
    val language= varchar("language", 128)
    val covered = text("covered")
    val courseInfo = reference("course_info_ref", CourseInfos)
    val fees = float("fees")
}
class CourseDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseDao>(Courses)

    var uuid by Courses.uuid
    var learningOutcomes by Courses.learningOutcomes
    var whoShouldAttend by Courses.whoShouldAttend
    var prerequisites by Courses.prerequisites
    var learningActivities by Courses.learningActivities
    var language by Courses.language
    var covered by Courses.covered
    var fees by Courses.fees
    var courseInfo by CourseInfoDao referencedOn Courses.courseInfo
}
object FellowShips: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val outline = text("outline")
    val fellowShipCourse = reference("course_ref", Courses)
    val courseInfo = reference("course_info_ref", CourseInfos)
}
class FellowShipDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FellowShipDao>(FellowShips)

    var uuid by FellowShips.uuid
    var outline by FellowShips.outline
    var fellowShipCourse by CourseDao referencedOn FellowShips.fellowShipCourse
    var courseInfo by CourseInfoDao referencedOn FellowShips.courseInfo
}
object Scholarships: IntIdTable(){
    val title = varchar("title", 255)
    val eligibility = text("eligibility")
    val benefits = text("benefits")
    val bondYears = integer("bond_years")
}
class ScholarshipDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ScholarshipDao>(Scholarships)

    var title by Scholarships.title
    var eligibility by Scholarships.eligibility
    var benefits by Scholarships.benefits
    var bondYears by Scholarships.bondYears
}
object Diplomas: IntIdTable(){
    val courseInfo = reference("course_info_ref", CourseInfos)
    val fees = float("fees")
    val outline = text("outline")
}
class DiplomaDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DiplomaDao>(Diplomas)

    var courseInfo by CourseInfoDao referencedOn Diplomas.courseInfo
    var fees by Diplomas.fees
    var outline by Diplomas.outline
}

object Employees: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val approvalStatus = integer("approval_status").clientDefault { 1 } // 0 rejected, 1 pending, 2 accepted
    val userType = integer("user_type") // 0 school head, 1 course manager 2, admin
    val userInfo = reference("user_reference", Persons)
}
class EmployeeDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EmployeeDao>(Employees)

    var uuid by Employees.uuid
    var approvalStatus by Employees.approvalStatus
    var userType by Employees.userType
    var userInfo by PersonDao referencedOn Employees.userInfo
}

object Participants: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val organisation = varchar("organisation", 255)
    val jobTitle = varchar("job_title", 255)
    val userInfo = reference("user_ref", Persons)
}
class ParticipantDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ParticipantDao>(Participants)

    var uuid by Participants.uuid
    var organisation by Participants.organisation
    var jobTitle by Participants.jobTitle
    var userInfo by PersonDao referencedOn Participants.userInfo
}

object Applications: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate()
    val progressType = integer("progress_type") // 0, rejected, 1 not approved, 2 in progress, 3 completed
    val participant = reference("participant_ref", Participants)
}
class ApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ApplicationDao>(Applications)

    var uuid by Applications.uuid
    var progressType by Applications.progressType
    var participant by ParticipantDao referencedOn Applications.participant
}

object CourseApplications: IntIdTable(){
    val applicationInfo = reference("application_ref", Participants)
    val course = reference("course_ref", Courses)
}
class CourseApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseApplicationDao>(CourseApplications)

    var applicationInfo by CourseApplications.applicationInfo
    var course by CourseDao referencedOn CourseApplications.course
}
object FellowShipApplications: IntIdTable(){
    val fellowShip = reference("fellowship_ref", FellowShips)
    val application = reference("application_ref", Applications)
}
class FellowShipApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FellowShipApplicationDao>(FellowShipApplications)

    var fellowship by FellowShipDao referencedOn FellowShipApplications.fellowShip
    var application by ApplicationDao referencedOn FellowShipApplications.application
}
object ScholarShipApplications: IntIdTable(){
    val scholarship = reference("scholarship_ref", Scholarships)
    val application = reference("application_ref", Applications)
}
class ScholarShipApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ScholarShipApplicationDao>(ScholarShipApplications)

    var scholarship by ScholarshipDao referencedOn ScholarShipApplications.scholarship
    var application by ApplicationDao referencedOn ScholarShipApplications.application
}
object DiplomaApplications: IntIdTable(){
    val diploma = reference("diploma_ref", Diplomas)
    val application = reference("application_ref", Applications)
}
class DiplomaApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DiplomaApplicationDao>(DiplomaApplications)

    var diploma by DiplomaDao referencedOn DiplomaApplications.diploma
    var application by ApplicationDao referencedOn DiplomaApplications.application
}

object CourseInfos: IntIdTable(){
    val title= varchar("title", 255)
    val startDate = long("start_date_timestamp")
    val endDate = long("end_date_timestamp")
    val applicationDeadline = long("application_deadline_timestamp")
}
class CourseInfoDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseInfoDao>(CourseInfos)

    var title by CourseInfos.title
    var startDate by CourseInfos.startDate
    var endDate by CourseInfos.endDate
    var applicationDeadline by CourseInfos.applicationDeadline
}


