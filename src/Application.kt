package com.saa.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.ArrayList

val argon2: Argon2 = Argon2Factory.create()
val server by lazy {
    embeddedServer(
        Netty,
        port = 8080,
        watchPaths = listOf(System.getProperty("user.dir")),
        module = Application::module
    )
}

fun setLoggingLevel(level: Level?) {
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = level
}

suspend fun argonHash(password: String): String {
    var returnString: String
    withContext(Dispatchers.IO) {
        returnString = argon2.hash(32, 65536, 4, password.toCharArray())
    }
    return returnString
}

fun main(args: Array<String>) {
    setLoggingLevel(Level.INFO)
    Runtime.getRuntime().addShutdownHook(Thread {
        stop()
    })
    dbConnect()
    printInfo("Starting Server...")
    // start the server
    server.start()
}

fun stop() {
    printInfo("Stopping Server... ( Wait 5 seconds! )")
    server.stop(5000, 10000)
    dbShutDown()
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        json(contentType = ContentType.Application.Json)
    }
    install(Compression)
    val testing = true
    routing {
        install(CachingHeaders) {
            options { outgoingContent ->
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60))
                    else -> null
                }
            }
        }
        if (testing) {
            get("/") {
                call.respondText("HELLO WORLD!")
            }
            get("/json") {
                call.respond(HelloWorld("world"))
            }
            post("/test-hash") {
                val password = call.receive<String>()
                withContext(Dispatchers.IO) {
                    call.respond(argonHash(password))
                }
            }
        }
        post("/login-staff") {
            // as a fallback, return nothing.
            var returnValue = ""
            val loginInfo = call.receive<LoginUserRoute>()
            printDebug(loginInfo.toString())
            newSuspendedTransaction(Dispatchers.IO) {
                val userInfo = PersonDao.find { Persons.email eq loginInfo.email }.first()
                printDebug(userInfo.email)
                if (argon2.verify(userInfo.passwordHash, loginInfo.password.toCharArray())) {
                    // ensure that user is an employee by counting the references
                    printDebug("correct password")
                    if (userInfo.employees.count() > 0 && userInfo.participants.count() == 0L) {
                        // ensure that the user is approved
                        printDebug("user is employee")
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
        data class ChangePasswordBody(var uuid: String, var password: String)
        post("/change-user-password") {
            // recieve the call and let kotlinx serialization deal with it.
            val body = call.receive<ChangePasswordBody>()
            newSuspendedTransaction(Dispatchers.IO) {
                val participant = ParticipantDao.find { Participants.uuid eq UUID.fromString(body.uuid) }.first()
                // hash the new password and update the database with it
                participant.userInfo.passwordHash = argonHash(body.password)
            }
        }
        post("/change-employee-password") {
            // recieve the call and let kotlinx serialization deal with it.
            val body = call.receive<ChangePasswordBody>()
            newSuspendedTransaction(Dispatchers.IO) {
                val employee = EmployeeDao.find { Employees.uuid eq UUID.fromString(body.uuid) }.first()
                // hash the new password and update the database with it
                employee.userInfo.passwordHash = argonHash(body.password)
            }
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
                val isSuccessful = newSuspendedTransaction {
                    try {
                        val courses = CourseDao.all()
                        for (course in courses) {
                            courseList.add(convertCourseDaoToCourseModel(course))
                        }
                        true
                    } catch (ex: Exception) {
                        printError(ex.toString())
                        false
                    }
                }
                if (!isSuccessful) {
                    throw Exception()
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
                val isSuccessful = newSuspendedTransaction {
                    try {
                        val fellowships = FellowShipDao.all()
                        for (fellowship in fellowships) {
                            fellowshipList.add(convertFellowshipDaoToFellowshipModel(fellowship))
                        }
                        true
                    } catch (ex: Exception) {
                        printError(ex.toString())
                        false
                    }
                }
                if (!isSuccessful) {
                    throw Exception()
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
                val isSuccessful = newSuspendedTransaction {
                    try {
                        val diplomas = DiplomaDao.all()
                        for (diploma in diplomas) {
                            diplomaList.add(convertDiplomaDaoToDiplomaModel(diploma))
                        }
                        true
                    } catch (ex: Exception) {
                        printError(ex.toString())
                        false
                    }
                }
                if (!isSuccessful) {
                    throw Exception()
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
                val isSuccessful = newSuspendedTransaction {
                    try {
                        val scholarships = ScholarshipDao.all()
                        // convert scholarships to the frontend model
                        for (scholarship in scholarships) {
                            scholarshipList.add(convertScholarshipDaoToScholarshipModel(scholarship))
                        }
                        true
                    } catch (ex: Exception) {
                        printError(ex.toString())
                        false
                    }
                }
                if (!isSuccessful) {
                    throw Exception()
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
                CourseApplicationDao.new {
                    participant =
                        ParticipantDao.find { Participants.uuid eq UUID.fromString(application.userUUID) }.first()
                    progressType = 1 // start off with not approved
                    course = courseDao
                }
            }
            call.respond(true)
        }
        post("/apply-fellowship") {
            val application = call.receive<FellowshipApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val fellowShipDao =
                    FellowShipDao.find { FellowShips.uuid eq UUID.fromString(application.fellowship.uuid) }.first()
                // create new application
                FellowShipApplicationDao.new {
                    participant =
                        ParticipantDao.find { Participants.uuid eq UUID.fromString(application.userUUID) }.first()
                    progressType = 1 // start off with not approved
                    fellowship = fellowShipDao
                }
            }
            call.respond(true)
        }
        post("/apply-scholarship") {
            val application = call.receive<ScholarshipApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val scholarshipDao =
                    ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(application.scholarship.uuid) }.first()
                // create new application
                ScholarShipApplicationDao.new {
                    participant =
                        ParticipantDao.find { Participants.uuid eq UUID.fromString(application.userUUID) }.first()
                    progressType = 1 // start off with not approved
                    scholarship = scholarshipDao
                }
                call.respond(true)
            }

        }
        post("/apply-diploma") {
            val application = call.receive<DiplomaApplyModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                val diplomaDao = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(application.diploma.uuid) }.first()
                // create new application
                DiplomaApplicationDao.new {
                    participant =
                        ParticipantDao.find { Participants.uuid eq UUID.fromString(application.userUUID) }.first()
                    progressType = 1 // start off with not approved
                    diploma = diplomaDao
                }
            }
            call.respond(true)
        }
        post("/update-application-progress") {
            // this will be the application uuid
            val userApplicationModel = call.receive<UserApplicationModel>()
            newSuspendedTransaction(Dispatchers.IO) {
                // I am using courseApplication index to figure out what kind of course it is and then update accordingly, hacky? I know.
                when (userApplicationModel.courseApplicationIndex) {
                    0 -> {
                        val courseApplication =
                            CourseApplicationDao.find { CourseApplications.uuid eq UUID.fromString(userApplicationModel.applicationUUID) }
                                .first()
                        courseApplication.progressType = userApplicationModel.progressType
                    }
                    1 -> {
                        val fellowshipApplication = FellowShipApplicationDao.find {
                            FellowShipApplications.uuid eq UUID.fromString(userApplicationModel.applicationUUID)
                        }.first()
                        fellowshipApplication.progressType = userApplicationModel.progressType
                    }
                    2 -> {
                        val scholarshipApplication = ScholarShipApplicationDao.find {
                            ScholarShipApplications.uuid eq UUID.fromString(userApplicationModel.applicationUUID)
                        }.first()
                        scholarshipApplication.progressType = userApplicationModel.progressType
                    }
                    3 -> {
                        val diplomaApplication = DiplomaApplicationDao.find {
                            DiplomaApplications.uuid eq UUID.fromString(userApplicationModel.applicationUUID)
                        }.first()
                        diplomaApplication.progressType = userApplicationModel.progressType
                    }
                }
            }
            call.respond(true)
        }
        post("/send-participants-notification") {
            //TODO pair both apps with firebase
        }
        post("/update-staff-approval") {
            val user = call.receive<EmployeeModel>()
            withContext(Dispatchers.IO) {
                newSuspendedTransaction {
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
//         for some odd reason UUID strings from JSON are serialized with the "" appended to them, hence we will remove them using the
//          removeSurrounding method
        post("/course-applications") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getCourseApplicants(UUID.fromString(uuid)).filter { it.progressType == 1 }
            call.respond(call.respond(applications))
        }
        post("/fellowship-applications") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getFellowshipApplicants(UUID.fromString(uuid)).filter { it.progressType == 1 }
            call.respond(call.respond(applications))
        }
        post("/scholarship-applications") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getScholarshipApplicants(UUID.fromString(uuid)).filter { it.progressType == 1 }
            call.respond(call.respond(applications))
        }
        post("/diploma-applications") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getDiplomaApplications(UUID.fromString(uuid)).filter { it.progressType == 1 }
            call.respond(call.respond(applications))
        }

        post("/course-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getCourseApplicants(UUID.fromString(uuid)).filter { it.progressType > 1 }
            call.respond(call.respond(applications))
        }
        post("/fellowship-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getFellowshipApplicants(UUID.fromString(uuid)).filter { it.progressType > 1 }
            call.respond(call.respond(applications))
        }
        post("/scholarship-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getScholarshipApplicants(UUID.fromString(uuid)).filter { it.progressType > 1 }
            call.respond(call.respond(applications))
        }
        post("/diploma-applications-approved") {
            // requires UUID
            val uuid = call.receive<String>().removeSurrounding("\"", "\"")
            val applications = getDiplomaApplications(UUID.fromString(uuid)).filter { it.progressType > 1 }
            call.respond(call.respond(applications))
        }
        post("/get-participant-course-applications") {
            var userId = call.receive<String>()
            // for some odd reason the parsed string includes quotes, remove them.
            userId = userId.removeSurrounding("\"", "\"")
            call.respond(getParticipantCourseApplications(UUID.fromString(userId)))
        }
        post("/get-participant-fellowship-applications") {
            var userId = call.receive<String>()
            userId = userId.removeSurrounding("\"", "\"")
            call.respond(getParticipantFellowshipApplications(UUID.fromString(userId)))
        }
        post("/get-participant-diploma-applications") {
            var userId = call.receive<String>()
            userId = userId.removeSurrounding("\"", "\"")
            call.respond(getParticipantDiplomaApplications(UUID.fromString(userId)))
        }
        post("/get-participant-scholarship-applications") {
            var userId = call.receive<String>()
            userId = userId.removeSurrounding("\"", "\"")
            call.respond(getParticipantScholarshipApplications(UUID.fromString(userId)))
        }
        post("/login-participant") {
            // as a fallback, return nothing.
            var returnValue = ""
            val loginInfo = call.receive<LoginUserRoute>()
            printDebug(loginInfo.toString())
            newSuspendedTransaction(Dispatchers.IO) {
                val userInfo = PersonDao.find { Persons.email eq loginInfo.email }.first()
                printDebug(userInfo.email)
                if (argon2.verify(userInfo.passwordHash, loginInfo.password.toCharArray())) {
                    if (userInfo.participants.count() > 0 && userInfo.employees.count() == 0L) {
                        // ensure that the user is approved
                        val participant = userInfo.participants.first()
                        returnValue = participant.uuid.toString()
                    }
                }
            }
            call.respond(returnValue)
        }

        post("/sign-up-participant") {
            val userParticipant = call.receive<UserParticipant>()
            createParticipantFromUser(userParticipant)
            call.respond(true)
        }

        post("/get-interests") {
            val returnList = ArrayList<InterestModel>()
            newSuspendedTransaction {

            }
        }

    }
}

