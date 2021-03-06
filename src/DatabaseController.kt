package com.saa.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
val dataSource by lazy {
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://localhost/saa_db"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username = "hubble_user"
        password = """.5k]A9z4[k?VJszna%{]Zc89=LYVPKD"""
        maximumPoolSize = 20
        minimumIdle = 5
    })
}
fun dbConnect() {
    Database.connect(dataSource)
    // create the schema
    printInfo("Creating db schema (will automatically skip if already created)")
    transaction {
        try {
            SchemaUtils.create(
                Persons,
                Participants,
                Employees,
                CourseInfos,
                Courses,
                FellowShips,
                Diplomas,
                Scholarships,
                FellowShipApplications,
                CourseApplications,
                ScholarShipApplications,
                DiplomaApplications,
                CourseInterests,
                FellowShipInterests,
                ScholarShipInterests,
                DiplomaInterests

            )
            SchemaUtils.createMissingTablesAndColumns(
                Persons,
                Participants,
                Employees,
                CourseInfos,
                Courses,
                FellowShips,
                Diplomas,
                Scholarships,
                FellowShipApplications,
                CourseApplications,
                ScholarShipApplications,
                DiplomaApplications
            )
        } catch (ex: Exception) {
            // just print the exception when it happens.
            printError(ex.toString())
        }
    }
}

fun dbShutDown() {
    printInfo("Shutting down HikariPool Connections")
    dataSource.close()
}

suspend fun createParticipantFromUser(user: UserParticipant) {
    withContext(Dispatchers.IO) {
        val hash = argonHash(user.password)
        val isSuccessful = newSuspendedTransaction {
            try {
                val person = PersonDao.new {
                    firstName = user.firstName
                    middleName = null
                    lastName = user.lastName
                    email = user.email
                    dateOfBirth = user.dob
                    passwordHash = hash
                    passportNumber = user.passportNumber
                    passportExpiry = user.passportExpiry
                    country = user.country
                    contactNumber = user.contactNumber.toString()
                    notificationToken = "0"
                }
                ParticipantDao.new {
                    userInfo = person
                    organisation = user.organisation
                    jobTitle = user.jobTitle
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
}

fun convertParticipantFromParticipantDao(participantDao: ParticipantDao): UserParticipant {
    return UserParticipant(
        participantDao.uuid.toString(),
        participantDao.userInfo.firstName,
        participantDao.userInfo.lastName,
        participantDao.userInfo.dateOfBirth,
        participantDao.userInfo.email,
        participantDao.userInfo.country,
        participantDao.userInfo.passportNumber,
        participantDao.userInfo.passportExpiry!!,
        participantDao.organisation,
        participantDao.jobTitle,
        "",
        participantDao.userInfo.contactNumber.toInt()
    )
}

suspend fun createEmployeeFromUser(user: UserStaff) {
    withContext(Dispatchers.IO) {
        val hash = argonHash(user.password)
        newSuspendedTransaction {
            val person = PersonDao.new {
                firstName = user.firstName
                middleName = user.middleName
                lastName = user.lastName
                email = user.email
                dateOfBirth = user.dateOfBirth
                passwordHash = hash
                passportNumber = user.passportNumber
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

suspend fun updateEmployeeFromUser(user: UserStaff) {
    withContext(Dispatchers.IO) {
        // leave the hash blank if update did not provide a new password
        val hash = if (!user.password.isBlank()) argonHash(user.password) else ""
        newSuspendedTransaction {
            try {
                val employee = EmployeeDao.find { Employees.uuid eq UUID.fromString(user.uuid) }.first()
                val person = employee.userInfo
                person.firstName = user.firstName
                person.middleName = user.middleName
                person.lastName = user.lastName
                person.email = user.email
                person.dateOfBirth = user.dateOfBirth
                // avoid making password hash blank
                if (hash.isNotBlank()) {
                    person.passwordHash = hash
                }
                person.passportNumber = user.passportNumber
                person.country = user.country
                person.contactNumber = user.contactNumber.toString()
                person.notificationToken = "0"
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

suspend fun getEmployeeInfo(uuid: String): UserStaff {
    var returnStaff = UserStaff()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val employee = EmployeeDao.find { Employees.uuid eq UUID.fromString(uuid) }.first()
                returnStaff = UserStaff(
                        employee.uuid.toString(),
                        employee.userInfo.firstName,
                        employee.userInfo.middleName,
                        employee.userInfo.lastName,
                        employee.userType,
                        employee.userInfo.email,
                        employee.userInfo.passportNumber,
                        employee.userInfo.dateOfBirth,
                        employee.userInfo.country,
                        "",
                        employee.userInfo.contactNumber.toInt()
                )
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
    return returnStaff
}

fun convertCourseDaoToCourseModel(courseDao: CourseDao): CourseModel {
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
            courseDao.covered,
            courseDao.whoShouldAttend,
            courseDao.courseInfo.applicationDeadline
    )
}

suspend fun createCourseFromCourseModel(courseModel: CourseModel) {
    // launch in a coroutine
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val info = CourseInfoDao.new {
                    title = courseModel.title
                    startDate = courseModel.startDate
                    endDate = courseModel.endDate
                    applicationDeadline = courseModel.applicationDeadline
                }
                CourseDao.new {
                    courseInfo = info
                    learningOutcomes = courseModel.learningOutcomes
                    whoShouldAttend = courseModel.attending
                    prerequisites = courseModel.prerequisites
                    learningActivities = courseModel.learningActivities
                    covered = courseModel.covered
                    fees = courseModel.fees
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

suspend fun updateCourseFromCourseModel(courseModel: CourseModel) {
    // launch in a coroutine
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
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
                course.covered = courseModel.covered
                course.fees = courseModel.fees
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

suspend fun deleteCourseFromCourseModel(courseModel: CourseModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val course = CourseDao.find { Courses.uuid eq UUID.fromString(courseModel.uuid) }.first()
                course.delete()
                course.courseInfo.delete()
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
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
        newSuspendedTransaction {
            try {
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
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
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
        newSuspendedTransaction {
            try {
                val fellowship =
                        FellowShipDao.find { FellowShips.uuid eq UUID.fromString(fellowshipModel.uuid) }.first()
                fellowship.courseInfo.applicationDeadline = fellowshipModel.applicationDeadline
                fellowship.courseInfo.title = fellowshipModel.title
                fellowship.outline = fellowshipModel.outline
                fellowship.fellowShipCourse =
                        CourseDao.find { Courses.uuid eq UUID.fromString(fellowshipModel.course.uuid) }.first()
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

/**
 * Deletes the fellowship from database using the uuid from the frontend
 * @param fellowshipModel the deserialized frontend class
 */
suspend fun deleteFellowshipFromFellowshipModel(fellowshipModel: FellowshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val fellowship =
                        FellowShipDao.find { FellowShips.uuid eq UUID.fromString(fellowshipModel.uuid) }.first()
                fellowship.delete()
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
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
        newSuspendedTransaction {
            try {
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
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
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
        newSuspendedTransaction {
            try {
                val diploma = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(diplomaModel.uuid) }.first()
                diploma.courseInfo.title = diplomaModel.title
                diploma.courseInfo.startDate = diplomaModel.startDate
                diploma.courseInfo.endDate = diplomaModel.endDate
                diploma.courseInfo.applicationDeadline = diplomaModel.applicationDeadline
                diploma.outline = diplomaModel.outline
                diploma.fees = diplomaModel.fees
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

/**
 * Deletes a field from database using the deserialized frontend model
 * @param diplomaModel the deserialized frontend model
 */
suspend fun deleteDiplomaFromDiplomaModel(diplomaModel: DiplomaModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val diploma = DiplomaDao.find { Diplomas.uuid eq UUID.fromString(diplomaModel.uuid) }.first()
                diploma.delete()
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
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
        newSuspendedTransaction {
            try {
                ScholarshipDao.new {
                    title = scholarshipModel.title
                    eligibility = scholarshipModel.eligibility
                    benefits = scholarshipModel.benefits
                    bondYears = scholarshipModel.bondTime
                    outline = scholarshipModel.outline
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
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
        newSuspendedTransaction {
            try {
                val scholarship =
                        ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(scholarshipModel.uuid) }.first()
                scholarship.title = scholarshipModel.title
                scholarship.eligibility = scholarshipModel.eligibility
                scholarship.benefits = scholarshipModel.benefits
                scholarship.bondYears = scholarshipModel.bondTime
                scholarship.outline = scholarshipModel.outline
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

/**
 * Delete a scholarship entry in the database from the deserialized frontend model
 * @param scholarshipModel deserialized frontend model
 */
suspend fun deleteScholarshipFromScholarshipModel(scholarshipModel: ScholarshipModel) {
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val scholarship =
                        ScholarshipDao.find { Scholarships.uuid eq UUID.fromString(scholarshipModel.uuid) }.first()
                scholarship.delete()
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
}

/**
 * get all the applicants for a course by its UUID
 * @param uuid unique uuid of the course
 */
suspend fun getCourseApplicants(uuid: UUID): List<UserApplicationModel> {
    val returnList = mutableListOf<UserApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val course = CourseDao.find { Courses.uuid eq uuid }.first()
                for (application in course.applications) {
                    val userInfo = application.participant.userInfo
                    val fullName =
                        if (userInfo.middleName != null) "${userInfo.firstName} ${userInfo.middleName} ${userInfo.lastName}" else "${userInfo.firstName} ${userInfo.lastName}"
                    returnList.add(
                        UserApplicationModel(
                            fullName,
                            application.progressType,
                            application.uuid.toString(),
                            convertParticipantFromParticipantDao(application.participant),
                            0 // 0 course, 1 fellowship, 2 scholarship, 3 diploma
                        )
                    )
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

/**
 * get all the applicants for a fellowship by its UUID
 * @param uuid unique uuid of the fellowship
 */
suspend fun getFellowshipApplicants(uuid: UUID): List<UserApplicationModel> {
    val returnList = mutableListOf<UserApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val fellowship = FellowShipDao.find { FellowShips.uuid eq uuid }.first()
                for (application in fellowship.applications) {
                    val userInfo = application.participant.userInfo
                    val fullName =
                        if (userInfo.middleName != null) "${userInfo.firstName} ${userInfo.middleName} ${userInfo.lastName}" else "${userInfo.firstName} ${userInfo.lastName}"
                    returnList.add(
                        UserApplicationModel(
                            fullName,
                            application.progressType,
                            application.uuid.toString(),
                            convertParticipantFromParticipantDao(application.participant),
                            1 // 0 course, 1 fellowship, 2 scholarship, 3 diploma
                        )
                    )
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

/**
 * get all the applicants for a scholarship by its UUID
 * @param uuid unique uuid of the scholarship
 */
suspend fun getScholarshipApplicants(uuid: UUID): List<UserApplicationModel> {
    val returnList = mutableListOf<UserApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val scholarship = ScholarshipDao.find { Scholarships.uuid eq uuid }.first()
                for (application in scholarship.applications) {
                    val userInfo = application.participant.userInfo
                    val fullName =
                        if (userInfo.middleName != null) "${userInfo.firstName} ${userInfo.middleName} ${userInfo.lastName}" else "${userInfo.firstName} ${userInfo.lastName}"
                    returnList.add(
                        UserApplicationModel(
                            fullName,
                            application.progressType,
                            application.uuid.toString(),
                            convertParticipantFromParticipantDao(application.participant),
                            2 // 0 course, 1 fellowship, 2 scholarship, 3 diploma
                        )
                    )
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

/**
 * get all the applicants for a diploma by its UUID
 * @param uuid unique uuid of the diploma
 */
suspend fun getDiplomaApplications(uuid: UUID): List<UserApplicationModel> {
    val returnList = mutableListOf<UserApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val diploma = DiplomaDao.find { Diplomas.uuid eq uuid }.first()
                for (application in diploma.applications) {
                    val userInfo = application.participant.userInfo
                    val fullName =
                        if (userInfo.middleName != null) "${userInfo.firstName} ${userInfo.middleName} ${userInfo.lastName}" else "${userInfo.firstName} ${userInfo.lastName}"
                    returnList.add(
                        UserApplicationModel(
                            fullName,
                            application.progressType,
                            application.uuid.toString(),
                            convertParticipantFromParticipantDao(application.participant),
                            2 // 0 course, 1 fellowship, 2 scholarship, 3 diploma
                        )
                    )
                }
            } catch (ex: ExposedSQLException) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

suspend fun getParticipantCourseApplications(uuid: UUID): List<CourseApplicationModel> {
    val returnList = mutableListOf<CourseApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction {
            try {
                val courseApplications = ParticipantDao.find { Participants.uuid eq uuid }.first().courseApplications
                for (application in courseApplications) {
                    // convert it to something the frontend understands
                    returnList.add(
                        CourseApplicationModel(
                            convertCourseDaoToCourseModel(application.course),
                            application.progressType
                        )
                    )
                }
            } catch (ex: Exception) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

suspend fun getParticipantFellowshipApplications(uuid: UUID): List<FellowshipApplicationModel> {
    val returnList = mutableListOf<FellowshipApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            try {
                val fellowshipApplications =
                    ParticipantDao.find { Participants.uuid eq uuid }.first().fellowshipApplications
                for (application in fellowshipApplications) {
                    // convert it to something the frontend understands
                    returnList.add(
                        FellowshipApplicationModel(
                            convertFellowshipDaoToFellowshipModel(application.fellowship),
                            application.progressType
                        )
                    )
                }
            } catch (ex: Exception) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

suspend fun getParticipantScholarshipApplications(uuid: UUID): List<ScholarshipApplicationModel> {
    val returnList = mutableListOf<ScholarshipApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            try {
                val scholarshipApplications =
                    ParticipantDao.find { Participants.uuid eq uuid }.first().scholarshipApplications
                for (application in scholarshipApplications) {
                    // convert it to something the frontend understands
                    returnList.add(
                        ScholarshipApplicationModel(
                            convertScholarshipDaoToScholarshipModel(application.scholarship),
                            application.progressType
                        )
                    )
                }
            } catch (ex: Exception) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}

suspend fun getParticipantDiplomaApplications(uuid: UUID): List<DiplomaApplicationModel> {
    val returnList = mutableListOf<DiplomaApplicationModel>()
    withContext(Dispatchers.IO) {
        newSuspendedTransaction(Dispatchers.IO) {
            try {
                val diplomaApplicationModel =
                    ParticipantDao.find { Participants.uuid eq uuid }.first().diplomaApplications
                for (application in diplomaApplicationModel) {
                    // convert it to something the frontend understands
                    returnList.add(
                        DiplomaApplicationModel(
                            convertDiplomaDaoToDiplomaModel(application.diploma),
                            application.progressType
                        )
                    )
                }
            } catch (ex: Exception) {
                printError(ex.toString())
            }
        }
    }
    return returnList
}