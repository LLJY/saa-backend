package com.saa.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun dbConnect(){
    val sqlSecret = """.5k]A9z4[k?VJszna%{]Zc89=LYVPKD"""
    val config = HikariConfig().apply {
        jdbcUrl         = "jdbc:mysql://localhost/saa_db"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username        = "hubble_user"
        password        = sqlSecret
        maximumPoolSize = 10
    }
    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
    // create the schema
    println("Creating db schema (will automatically skip if already created)")
    transaction {
        SchemaUtils.create(Persons, Participants, Employees, CourseInfos, Courses, FellowShips, Diplomas, Scholarships, Applications, FellowShipApplications, CourseApplications, ScholarShipApplications, DiplomaApplications)
    }
}
fun createEmployeeFromUser(user: UserStaff){
    GlobalScope.launch(Dispatchers.IO) {
        transaction {
            val person = PersonDao.new {
                firstName = user.firstName
                middleName = user.middleName
                lastName = user.lastName
                email = user.email
                dateOfBirth = user.dateOfBirth
                passwordHash = argonHash(user.password)
                passportNumber = user.passportNumber
                passportExpiry = user.passportExpiry
                country = user.country
                contactNumber = user.contactNumber.toString()
                notificationToken = "0"
            }
            EmployeeDao.new {
                userType = user.userLevel
                userInfo = person
            }
        }
    }
}
fun convertCourseDaoToCourseModel(courseDao: CourseDao): CourseModel{
    return CourseModel(
            courseDao.uuid.toString(),
            courseDao.courseInfo.title,
            courseDao.courseInfo.startDate,
            courseDao.courseInfo.endDate,
            courseDao.fees,
            courseDao.learningOutcomes,
            courseDao.prerequisites,
            courseDao.learningActivities,
            courseDao.language,
            courseDao.covered,
            courseDao.whoShouldAttend,
            courseDao.courseInfo.applicationDeadline
    )
}
fun createCourseFromCourseModel(courseModel: CourseModel){
    // launch in a coroutine
    // TODO use deferred to catch any errors and return it to the user
    GlobalScope.launch(Dispatchers.IO) {
        transaction {
            val info = CourseInfoDao.new {
                title = courseModel.title
                startDate = courseModel.startDate
                endDate = courseModel.endDate
                applicationDeadline = courseModel.applicationDeadline
            }
            val course = CourseDao.new {
                courseInfo = info
                learningOutcomes = courseModel.learningOutcomes
                whoShouldAttend = courseModel.attending
                prerequisites = courseModel.prerequisites
                learningActivities = courseModel.learningActivities
                language = courseModel.language
                covered = courseModel.covered
                fees = courseModel.fees
            }
        }
    }
}

fun updateCourseFromCourseModel(courseModel: CourseModel){
    // launch in a coroutine
    // TODO use deferred to catch any errors and return it to the user
    GlobalScope.launch {
        transaction {
            // take the first one, uuid is unique
            val course = CourseDao.find{Courses.uuid eq UUID.fromString(courseModel.uuid)}.first()
            // update all the fields
            course.courseInfo.title = courseModel.title
            course.courseInfo.startDate = courseModel.startDate
            course.courseInfo.endDate = courseModel.endDate
            course.courseInfo.applicationDeadline = courseModel.applicationDeadline
            course.learningOutcomes = courseModel.learningOutcomes
            course.whoShouldAttend = courseModel.attending
            course.prerequisites = courseModel.prerequisites
            course.learningActivities = courseModel.learningActivities
            course.language = courseModel.language
            course.covered = courseModel.covered
            course.fees = courseModel.fees
        }
    }
}

fun deleteCourseFromCourseModel(courseModel: CourseModel){
    GlobalScope.launch(Dispatchers.IO) {
        transaction {
            val course = CourseDao.find {Courses.uuid eq UUID.fromString(courseModel.uuid)}.first()
            course.delete()
            course.courseInfo.delete()
        }
    }
}






