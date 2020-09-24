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
        post("/login-staff"){
            var returnValue = false
            val loginInfo = call.receive<LoginUserRoute>()
            println(loginInfo.toString())
            transaction {
                val userInfo = PersonDao.find { Persons.email eq loginInfo.email }.first()
                if(argon2.verify(userInfo.passwordHash, loginInfo.password.toCharArray())){
                    // ensure that user is an employee by counting the references
                    if(userInfo.employees.count() > 0 && userInfo.participants.count() == 0L){
                        // ensure that the user is approved
                        if(userInfo.employees.first().approvalStatus == 2){
                            returnValue = true
                        }
                    }
                }
            }
            call.respond(returnValue)
        }
        // just respond booleans, a 500 will trigger retrofit to error out
        post("/add-course"){
            createCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        post("/delete-course"){
            deleteCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        post("/update-course"){
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
                    for (scholarship in scholarships) {
                        scholarshipList.add(convertScholarshipDaoToScholarshipModel(scholarship))
                    }
                }
            }
            // assert the type for serialization
            call.respond(scholarshipList)
        }


        post("/approve-staff") {
            val user = call.receive<UserStaff>()
            withContext(Dispatchers.IO) {
                transaction {
                    val employee = EmployeeDao.find { Employees.uuid eq UUID.fromString(user.uuid) }.first()
                    employee.approvalStatus = 2
                }
            }
        }
        post("/sign-up-staff"){
            val newUser = call.receive<UserStaff>()
            createEmployeeFromUser(newUser)
            call.respond("true")
        }

        get("/hello-world") {
            call.respondText("HELLO WORLD!")
        }

        get("/json/gson") {
            call.respond(com.saa.backend.HelloWorld("world"))
        }
    }
}

