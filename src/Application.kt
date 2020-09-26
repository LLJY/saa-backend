package com.saa.backend

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.ArrayList

val argon2: Argon2 = Argon2Factory.create()
suspend fun argonHash(password: String): String {
    var returnString = ""
    withContext(Dispatchers.IO) {
        returnString = argon2.hash(32, 65536, 4, password.toCharArray())
    }
    return returnString
}

fun main(args: Array<String>) {
    dbConnect()
    embeddedServer(
            Netty,
            port = 8080,
            watchPaths = listOf(System.getProperty("user.dir")),
            module = Application::module
    ).start(wait = true)
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        json(contentType = ContentType.Application.Json)
    }

    routing {
        post("/login-staff") {
            // as a fallback, return nothing.
            var returnValue: String = ""
            val loginInfo = call.receive<LoginUserRoute>()
            println(loginInfo.toString())
            newSuspendedTransaction(Dispatchers.IO) {
                val userInfo = PersonDao.find { Persons.email eq loginInfo.email }.first()
                println(userInfo.email)
                if (argon2.verify(userInfo.passwordHash, loginInfo.password.toCharArray())) {
                    // ensure that user is an employee by counting the references
                    println("correct password")
                    if (userInfo.employees.count() > 0 && userInfo.participants.count() == 0L) {
                        // ensure that the user is approved
                        println("user is employee")
                        val employee = userInfo.employees.first()
                        if (employee.approvalStatus == 2) {
                            // return the uuid
                            returnValue = employee.uuid.toString()
                        }
                    }
                }
            }
            call.respond(returnValue)
        }
        post("/get-employee-info") {
            // respond with get employee info
            var uuid = call.receive<String>()
            //remove the first and last characters as for some reason kotlinx serialization does not remove the "" from the string
            uuid = uuid.substring(1, uuid.length - 1)
            call.respond(getEmployeeInfo(uuid))
        }
        post("/update-employee-info") {
            // kotlin smart casting will deal with the types for us
            updateEmployeeFromUser(call.receive())
            call.respond(true)
        }
        // just respond booleans, a 500 will trigger retrofit to error out
        post("/add-course") {
            createCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        post("/delete-course") {
            deleteCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        post("/update-course") {
            updateCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        get("/get-courses"){
            val courseList = ArrayList<CourseModel>()
            withContext(Dispatchers.IO) {
                transaction {
                    val courses = CourseDao.all()
                    for (course in courses) {
                        courseList.add(convertCourseDaoToCourseModel(course))
                    }

                }
            }
            // assert the type for serialization
            call.respond(ArrayList<CourseModel>(courseList))
        }
        post("/add-fellowship") {
            createFellowshipFromFellowshipModel(call.receive())
            call.respond(true)
        }
        post("/delete-fellowship") {
            deleteFellowshipFromFellowshipModel(call.receive())
            call.respond(true)
        }
        post("/update-fellowship") {
            updateFellowshipFromFellowshipModel(call.receive())
            call.respond(true)
        }
        get("/get-fellowships") {
            val fellowshipList = ArrayList<FellowshipModel>()
            withContext(Dispatchers.IO) {
                transaction {
                    val fellowships = FellowShipDao.all()
                    for (fellowship in fellowships) {
                        fellowshipList.add(convertFellowshipDaoToFellowshipModel(fellowship))
                    }
                }
            }
            // assert the type for serialization
            call.respond(ArrayList<FellowshipModel>(fellowshipList))
        }
        post("/add-diploma") {
            createDiplomaFromDiplomaModel(call.receive())
            call.respond(true)
        }
        post("/delete-diploma") {
            deleteDiplomaFromDiplomaModel(call.receive())
            call.respond(true)
        }
        post("/update-diploma") {
            updateDiplomaFromDiplomaModel(call.receive())
            call.respond(true)
        }
        get("/get-diplomas") {
            val diplomaList = ArrayList<DiplomaModel>()
            withContext(Dispatchers.IO) {
                transaction {
                    val diplomas = DiplomaDao.all()
                    for (diploma in diplomas) {
                        diplomaList.add(convertDiplomaDaoToDiplomaModel(diploma))
                    }
                }
            }
            // assert the type for serialization
            call.respond(ArrayList<DiplomaModel>(diplomaList))
        }
        post("/add-scholarship") {
            createScholarshipFromScholarshipModel(call.receive())
            call.respond(true)
        }
        post("/delete-scholarship") {
            deleteScholarshipFromScholarshipModel(call.receive())
            call.respond(true)
        }
        post("/update-scholarship") {
            updateScholarshipFromScholarshipModel(call.receive())
            call.respond(true)
        }
        get("/get-scholarships") {
            val scholarshipList = ArrayList<ScholarshipModel>()
            withContext(Dispatchers.IO) {
                transaction {
                    val scholarships = ScholarshipDao.all()
                    // convert scholarships to the frontend model
                    for (scholarship in scholarships) {
                        scholarshipList.add(convertScholarshipDaoToScholarshipModel(scholarship))
                    }
                }
            }
            // assert the type for serialization
            call.respond(scholarshipList)
        }
        get("/get-unapproved-staff") {
            withContext(Dispatchers.IO) {
                val returnStaffs = ArrayList<EmployeeModel>()
                newSuspendedTransaction {
                    // get the staff that are
                    val unApprovedStaff = EmployeeDao.find { Employees.approvalStatus eq 1 }
                    for (staff in unApprovedStaff) {
                        // gather all the information and add them to a list
                        val userInfo = staff.userInfo
                        val fullName = if (userInfo.middleName != null) "${userInfo.firstName} ${userInfo.middleName} ${userInfo.lastName}" else "${userInfo.firstName} ${userInfo.lastName}"
                        returnStaffs.add(
                                EmployeeModel(
                                        staff.uuid.toString(),
                                        fullName,
                                        userInfo.dateOfBirth,
                                        userInfo.contactNumber,
                                        userInfo.country,
                                        userInfo.passportNumber,
                                        userInfo.passportExpiry,
                                        userInfo.email,
                                        when (staff.userType) {
                                            0 -> "School Head"
                                            1 -> "Course Manager"
                                            2 -> "Admin"
                                            else -> "Unknown"
                                        },
                                        staff.approvalStatus
                                )
                        )
                    }
                }
                call.respond(returnStaffs)
            }
        }
        // expect both UUID and Course model
        post("/apply-course") {
            val application = call.receive<CourseApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val courseDao = CourseDao.find { Courses.uuid eq UUID.fromString(application.course.uuid) }.first()
                // create new application
                val applicationDao = ApplicationDao.new {
                    progressType = 1 // start off with not approved
                    participant = ParticipantDao.find { Participants.uuid eq UUID.fromString(application.userUUID) }.first()
                }
                val courseApplicationDao = CourseApplicationDao.new {
                    applicationInfo = applicationDao
                    course = courseDao
                }
            }
        }
        post("/apply-fellowship") {
            val userApplication = call.receive<FellowshipApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val fellowShipDao = FellowShipDao.find { FellowShips.uuid eq UUID.fromString(userApplication.fellowship.uuid) }.first()
                // create new application
                val applicationDao = ApplicationDao.new {
                    progressType = 1 // start off with not approved
                    participant = ParticipantDao.find { Participants.uuid eq UUID.fromString(userApplication.userUUID) }.first()
                }
                val fellowShipApplicationDao = FellowShipApplicationDao.new {
                    application = applicationDao
                    fellowship = fellowShipDao
                }
            }
        }
        post("/apply-scholarship") {
            val userApplication = call.receive<ScholarshipApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val scholarshipDao = ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(userApplication.scholarship.uuid) }.first()
                // create new application
                val applicationDao = ApplicationDao.new {
                    progressType = 1 // start off with not approved
                    participant = ParticipantDao.find { Participants.uuid eq UUID.fromString(userApplication.userUUID) }.first()
                }
                val fellowShipApplicationDao = ScholarShipApplicationDao.new {
                    application = applicationDao
                    scholarship = scholarshipDao
                }
            }

        }
        post("/apply-diploma") {
            val userApplication = call.receive<DiplomaApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val diplomaDao = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(userApplication.diploma.uuid) }.first()
                // create new application
                val applicationDao = ApplicationDao.new {
                    progressType = 1 // start off with not approved
                    participant = ParticipantDao.find { Participants.uuid eq UUID.fromString(userApplication.userUUID) }.first()
                }
                DiplomaApplicationDao.new {
                    application = applicationDao
                    diploma = diplomaDao
                }
            }

        }
        post("/send-participants-notification") {

        }
        post("/update-staff-approval") {
            val user = call.receive<EmployeeModel>()
            withContext(Dispatchers.IO) {
                transaction {
                    val employee = EmployeeDao.find { Employees.uuid eq UUID.fromString(user.uuid) }.first()
                    employee.approvalStatus = user.approvalStatus
                }
            }
            call.respond(true)
        }
        post("/sign-up-staff") {
            val newUser = call.receive<UserStaff>()
            createEmployeeFromUser(newUser)
            call.respond("true")
        }
        post("/course-applications") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getCourseApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/fellowship-applications") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getFellowshipApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/scholarship-applications") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getScholarshipApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/diploma-applications") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getDiplomaApplications(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }

        post("/course-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getCourseApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/fellowship-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getFellowshipApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/scholarship-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getScholarshipApplicants(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }
        post("/diploma-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>()
            val applications = getDiplomaApplications(UUID.fromString(uuid))
            call.respond(call.respond(applications))
        }

        get("/hello-world") {
            call.respondText("HELLO WORLD!")
        }

        get("/json/gson") {
            call.respond(com.saa.backend.HelloWorld("world"))
        }
        post("/test-hash") {
            val password = call.receive<String>()
            withContext(Dispatchers.IO) {
                call.respond(argonHash(password))
            }
        }

    }
}

