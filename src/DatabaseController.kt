package com.saa.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
        SchemaUtils.createMissingTablesAndColumns(Persons, Participants, Employees, CourseInfos, Courses, FellowShips, Diplomas, Scholarships, Applications, FellowShipApplications, CourseApplications, ScholarShipApplications, DiplomaApplications)
        SchemaUtils.create(Persons, Participants, Employees, CourseInfos, Courses, FellowShips, Diplomas, Scholarships, Applications, FellowShipApplications, CourseApplications, ScholarShipApplications, DiplomaApplications)
    }
}

suspend fun createEmployeeFromUser(user: UserStaff) {
    withContext(Dispatchers.IO) {
        val hash = argonHash(user.password)
        newSuspendedTransaction(Dispatchers.IO) {
            val person = PersonDao.new {
                firstName = user.firstName
                middleName = user.middleName
                lastName = user.lastName
                email = user.email
                dateOfBirth = user.dateOfBirth
                passwordHash = hash
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
    // courses do not have nullable fields
    return CourseModel(
            courseDao.uuid.toString(),
            courseDao.courseInfo.title,
            courseDao.courseInfo.startDate!!,
            courseDao.courseInfo.endDate!!,
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

suspend fun createCourseFromCourseModel(courseModel: CourseModel) {
    // launch in a coroutine
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
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

suspend fun updateCourseFromCourseModel(courseModel: CourseModel) {
    // launch in a coroutine
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            // take the first one, uuid is unique
            val course = CourseDao.find { Courses.uuid eq UUID.fromString(courseModel.uuid) }.first()
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

suspend fun deleteCourseFromCourseModel(courseModel: CourseModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val course = CourseDao.find { Courses.uuid eq UUID.fromString(courseModel.uuid) }.first()
            course.delete()
            course.courseInfo.delete()
        }
    }
}

/**
 * Converts the database object to something suitable for the frontend
 * @param fellowShipDao the database object
 */
fun convertFellowshipDaoToFellowshipModel(fellowShipDao: FellowShipDao): FellowshipModel {
    return FellowshipModel(
            fellowShipDao.uuid.toString(),
            fellowShipDao.courseInfo.title,
            fellowShipDao.outline,
            convertCourseDaoToCourseModel(fellowShipDao.fellowShipCourse),
            fellowShipDao.courseInfo.applicationDeadline
    )
}

/**
 * Creates a new entry in the database with the frontend model
 * @param fellowshipModel the deserialized frontend model
 */
suspend fun createFellowshipFromFellowshipModel(fellowshipModel: FellowshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            // get the course that matches the id
            val course = CourseDao.find { Courses.uuid eq UUID.fromString(fellowshipModel.course.uuid) }.first()
            val ci = CourseInfoDao.new {
                title = fellowshipModel.title
                applicationDeadline = fellowshipModel.applicationDeadline
            }
            // create the fellowship
            FellowShipDao.new {
                outline = fellowshipModel.outline
                courseInfo = ci
                fellowShipCourse = course
            }
        }
    }
}

/**
 * Updates the fellowship from database
 * @param fellowshipModel the deserialized frontend class
 */
suspend fun updateFellowshipFromFellowshipModel(fellowshipModel: FellowshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val fellowship = FellowShipDao.find { FellowShips.uuid eq UUID.fromString(fellowshipModel.uuid) }.first()
            fellowship.courseInfo.applicationDeadline = fellowshipModel.applicationDeadline
            fellowship.courseInfo.title = fellowshipModel.title
            fellowship.outline = fellowshipModel.outline
            fellowship.fellowShipCourse = CourseDao.find { Courses.uuid eq UUID.fromString(fellowshipModel.course.uuid) }.first()
        }
    }
}

/**
 * Deletes the fellowship from database using the uuid from the frontend
 * @param fellowshipModel the deserialized frontend class
 */
suspend fun deleteFellowshipFromFellowshipModel(fellowshipModel: FellowshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val fellowship = FellowShipDao.find { FellowShips.uuid eq UUID.fromString(fellowshipModel.uuid) }.first()
            fellowship.delete()
        }
    }
}

/**
 * Converts a database model to the frontend model
 * @param diplomaDao the database object
 */
fun convertDiplomaDaoToDiplomaModel(diplomaDao: DiplomaDao): DiplomaModel {
    return DiplomaModel(
            diplomaDao.uuid.toString(),
            diplomaDao.courseInfo.title,
            diplomaDao.fees,
            diplomaDao.outline,
            diplomaDao.courseInfo.startDate!!, // diploma models always have startDate and endDate.
            diplomaDao.courseInfo.endDate!!,
            diplomaDao.courseInfo.applicationDeadline
    )
}

/**
 * Adds a new field to the database from the deserialized frontend model
 * @param diplomaModel the deserialized frontend model
 */
suspend fun createDiplomaFromDiplomaModel(diplomaModel: DiplomaModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val ci = CourseInfoDao.new {
                title = diplomaModel.title
                startDate = diplomaModel.startDate
                endDate = diplomaModel.endDate
                applicationDeadline = diplomaModel.applicationDeadline
            }
            DiplomaDao.new {
                courseInfo = ci
                fees = diplomaModel.fees
                outline = diplomaModel.outline
            }
        }
    }
}

/**
 * Updates a field from database using the deserialized frontend model
 * @param diplomaModel the deserialized frontend model
 */
suspend fun updateDiplomaFromDiplomaModel(diplomaModel: DiplomaModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val diploma = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(diplomaModel.uuid) }.first()
            diploma.courseInfo.title = diplomaModel.title
            diploma.courseInfo.startDate = diplomaModel.startDate
            diploma.courseInfo.endDate = diplomaModel.endDate
            diploma.courseInfo.applicationDeadline = diplomaModel.applicationDeadline
            diploma.outline = diplomaModel.outline
            diploma.fees = diplomaModel.fees
        }
    }
}

/**
 * Deletes a field from database using the deserialized frontend model
 * @param diplomaModel the deserialized frontend model
 */
suspend fun deleteDiplomaFromDiplomaModel(diplomaModel: DiplomaModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val diploma = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(diplomaModel.uuid) }.first()
            diploma.delete()
        }
    }
}

/**
 * Converts the scholarship database object to the frontend model
 * @param scholarshipDao database access object
 */
fun convertScholarshipDaoToScholarshipModel(scholarshipDao: ScholarshipDao): ScholarshipModel {
    return ScholarshipModel(
            scholarshipDao.uuid.toString(),
            scholarshipDao.title,
            scholarshipDao.eligibility,
            scholarshipDao.benefits,
            scholarshipDao.bondYears,
            scholarshipDao.outline
    )
}

/**
 * Create a new scholarship entry in the database from the deserialized frontend model
 * @param scholarshipModel deserialized frontend model
 */
suspend fun createScholarshipFromScholarshipModel(scholarshipModel: ScholarshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            ScholarshipDao.new {
                title = scholarshipModel.title
                eligibility = scholarshipModel.eligibility
                benefits = scholarshipModel.benefits
                bondYears = scholarshipModel.bondTime
                outline = scholarshipModel.outline
            }
        }
    }
}

/**
 * Update a scholarship entry in the database from the deserialized frontend model
 * @param scholarshipModel deserialized frontend model
 */
suspend fun updateScholarshipFromScholarshipModel(scholarshipModel: ScholarshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val scholarship = ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(scholarshipModel.uuid) }.first()
            scholarship.title = scholarshipModel.title
            scholarship.eligibility = scholarshipModel.eligibility
            scholarship.benefits = scholarshipModel.benefits
            scholarship.bondYears = scholarshipModel.bondTime
            scholarship.outline = scholarshipModel.outline
        }
    }
}

/**
 * Delete a scholarship entry in the database from the deserialized frontend model
 * @param scholarshipModel deserialized frontend model
 */
suspend fun deleteScholarshipFromScholarshipModel(scholarshipModel: ScholarshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            val scholarship = ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(scholarshipModel.uuid) }.first()
            scholarship.delete()
        }
    }
}











