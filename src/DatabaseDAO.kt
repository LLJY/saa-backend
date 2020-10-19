package com.saa.backend

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption


fun getUnixTime(): Long{
    return System.currentTimeMillis()
}

object Persons: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()
    val lastName = varchar("last_name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val dateOfBirth = long("dob_timestamp")
    val passwordHash = varchar("password_hash", 255)
    val passportNumber = varchar("passport_number", 255)
    val contactNumber = varchar("contact_number", 25)
    val passportExpiry = long("passport_expiry").nullable()
    val country = varchar("country", 64)
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
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val learningOutcomes = text("learning_outcome")
    val whoShouldAttend = text("who_should_attend")
    val prerequisites = text("prerequisites")
    val learningActivities = text("learning_activities")
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
    var covered by Courses.covered
    var fees by Courses.fees
    var courseInfo by CourseInfoDao referencedOn Courses.courseInfo
    val applications by CourseApplicationDao referrersOn CourseApplications.course
}
object FellowShips: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
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
    val applications by FellowShipApplicationDao referrersOn FellowShipApplications.fellowShip
}
object Scholarships: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val title = varchar("title", 255)
    val eligibility = text("eligibility")
    val benefits = text("benefits")
    val bondYears = integer("bond_years")
    val outline = text("outline")
}
class ScholarshipDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ScholarshipDao>(Scholarships)

    var uuid by Scholarships.uuid
    var title by Scholarships.title
    var eligibility by Scholarships.eligibility
    var benefits by Scholarships.benefits
    var bondYears by Scholarships.bondYears
    var outline by Scholarships.outline
    val applications by ScholarShipApplicationDao referrersOn ScholarShipApplications.scholarship
}
object Diplomas: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val courseInfo = reference("course_info_ref", CourseInfos)
    val fees = float("fees")
    val outline = text("outline")
}
class DiplomaDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DiplomaDao>(Diplomas)

    var uuid by Diplomas.uuid
    var courseInfo by CourseInfoDao referencedOn Diplomas.courseInfo
    var fees by Diplomas.fees
    var outline by Diplomas.outline
    val applications by DiplomaApplicationDao referrersOn DiplomaApplications.diploma
}

object Employees: IntIdTable(){
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
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
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
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
    val courseApplications by CourseApplicationDao referrersOn CourseApplications.participant
    val fellowshipApplications by FellowShipApplicationDao referrersOn FellowShipApplications.participant
    val scholarshipApplications by ScholarShipApplicationDao referrersOn ScholarShipApplications.participant
    val diplomaApplications by DiplomaApplicationDao referrersOn DiplomaApplications.participant
    val courseInterests by CourseInterestDao referrersOn CourseInterests.participant
    val fellowshipInterests by FellowShipInterestDao referrersOn FellowShipInterests.participant
    val scholarshipInterests by ScholarShipInterestDao referrersOn ScholarShipInterests.participant
    val diplomaInterests by DiplomaInterestDao referrersOn DiplomaInterests.participant
}
object CourseApplications: IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val progressType = integer("progress_type") // 0, rejected, 1 not approved, 2 in progress, 3 completed
    val participant = reference("participant_ref", Participants, ReferenceOption.CASCADE)
    val course = reference("course_ref", Courses)
}

class CourseApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseApplicationDao>(CourseApplications)

    var uuid by CourseApplications.uuid
    var course by CourseDao referencedOn CourseApplications.course
    var progressType by CourseApplications.progressType // 0, rejected, 1 not approved, 2 in progress, 3 completed
    var participant by ParticipantDao referencedOn CourseApplications.participant
}
object FellowShipApplications: IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val fellowShip = reference("fellowship_ref", FellowShips, ReferenceOption.CASCADE)
    val progressType = integer("progress_type") // 0, rejected, 1 not approved, 2 in progress, 3 completed
    val participant = reference("participant_ref", Participants)
}
class FellowShipApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FellowShipApplicationDao>(FellowShipApplications)

    var uuid by FellowShipApplications.uuid
    var fellowship by FellowShipDao referencedOn FellowShipApplications.fellowShip
    var progressType by FellowShipApplications.progressType // 0, rejected, 1 not approved, 2 in progress, 3 completed
    var participant by ParticipantDao referencedOn FellowShipApplications.participant
}
object ScholarShipApplications: IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val scholarship = reference("scholarship_ref", Scholarships, ReferenceOption.CASCADE)
    val progressType = integer("progress_type") // 0, rejected, 1 not approved, 2 in progress, 3 completed
    val participant = reference("participant_ref", Participants)
}
class ScholarShipApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ScholarShipApplicationDao>(ScholarShipApplications)

    var uuid by ScholarShipApplications.uuid
    var scholarship by ScholarshipDao referencedOn ScholarShipApplications.scholarship
    var progressType by ScholarShipApplications.progressType // 0, rejected, 1 not approved, 2 in progress, 3 completed
    var participant by ParticipantDao referencedOn ScholarShipApplications.participant
}
object DiplomaApplications: IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val diploma = reference("diploma_ref", Diplomas, ReferenceOption.CASCADE)
    val progressType = integer("progress_type") // 0, rejected, 1 not approved, 2 in progress, 3 completed
    val participant = reference("participant_ref", Participants)
}

class DiplomaApplicationDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DiplomaApplicationDao>(DiplomaApplications)

    var uuid by DiplomaApplications.uuid
    var diploma by DiplomaDao referencedOn DiplomaApplications.diploma
    var progressType by DiplomaApplications.progressType // 0, rejected, 1 not approved, 2 in progress, 3 completed
    var participant by ParticipantDao referencedOn DiplomaApplications.participant
}

object CourseInterests : IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val participant = reference("participant_ref", Participants, ReferenceOption.CASCADE)
    val course = reference("course_ref", Courses)
}

class CourseInterestDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseInterestDao>(CourseInterests)

    var uuid by CourseInterests.uuid
    var course by CourseDao referencedOn CourseInterests.course
    var participant by ParticipantDao referencedOn CourseInterests.participant
}

object FellowShipInterests : IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val fellowShip = reference("fellowship_ref", FellowShips, ReferenceOption.CASCADE)
    val participant = reference("participant_ref", Participants)
}

class FellowShipInterestDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FellowShipInterestDao>(FellowShipInterests)

    var uuid by FellowShipInterests.uuid
    var fellowship by FellowShipDao referencedOn FellowShipInterests.fellowShip
    var participant by ParticipantDao referencedOn FellowShipInterests.participant
}

object ScholarShipInterests : IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val scholarship = reference("scholarship_ref", Scholarships, ReferenceOption.CASCADE)
    val participant = reference("participant_ref", Participants)
}

class ScholarShipInterestDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ScholarShipInterestDao>(ScholarShipInterests)

    var uuid by ScholarShipInterests.uuid
    var scholarship by ScholarshipDao referencedOn ScholarShipInterests.scholarship
    var participant by ParticipantDao referencedOn ScholarShipInterests.participant
}

object DiplomaInterests : IntIdTable() {
    val uuid = uuid("uuid").autoGenerate().uniqueIndex()
    val diploma = reference("diploma_ref", Diplomas, ReferenceOption.CASCADE)
    val participant = reference("participant_ref", Participants)
}

class DiplomaInterestDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<DiplomaInterestDao>(DiplomaInterests)

    var uuid by DiplomaInterests.uuid
    var diploma by DiplomaDao referencedOn DiplomaInterests.diploma
    var participant by ParticipantDao referencedOn DiplomaInterests.participant
}

object CourseInfos : IntIdTable() {
    val title = varchar("title", 255)
    val startDate = long("start_date_timestamp").nullable()
    val endDate = long("end_date_timestamp").nullable()
    val applicationDeadline = long("application_deadline_timestamp")
}

class CourseInfoDao(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseInfoDao>(CourseInfos)

    var title by CourseInfos.title
    var startDate by CourseInfos.startDate
    var endDate by CourseInfos.endDate
    var applicationDeadline by CourseInfos.applicationDeadline
}


