package com.saa.backend

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.transactions.transaction

val argon2: Argon2 = Argon2Factory.create()
fun argonHash(password: String): String{
    return argon2.hash(32, 65536, 4, password.toCharArray())
}
fun main(args: Array<String>){
    dbConnect()
    embeddedServer(
            Netty,
            port = 8080,
            watchPaths = listOf(System.getProperty("user.dir")),
            module = Application::module
    ).start(wait=true)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
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
        post("/add-course"){
            println("adding course...")
            val newCourse = call.receive<CourseModel>()
            createCourseFromCourseModel(newCourse)
            call.respond(true)
        }
        post("/delete-course"){
            deleteCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        post("/update-course"){
            println("updating course...")
            updateCourseFromCourseModel(call.receive())
            call.respond(true)
        }
        get("/get-courses"){
            val courseList = ArrayList<CourseModel>()
            transaction {
                val courses = CourseDao.all()
                for(course in courses){
                    courseList.add(convertCourseDaoToCourseModel(course))
                }
            }
            // assert the type for serialization
            call.respond(ArrayList<CourseModel>(courseList))
        }
        post("/approve-staff") {

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

