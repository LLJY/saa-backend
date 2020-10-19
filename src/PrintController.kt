package com.saa.backend

import org.slf4j.LoggerFactory

const val ANSI_RESET = "\u001B[0m"
const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"
val logger = LoggerFactory.getLogger("")
fun printInfo(text: String) {
    logger.info("$ANSI_GREEN$text$ANSI_RESET")
}

fun printDebug(text: String) {
    logger.debug("$ANSI_CYAN$text$ANSI_RESET")
}

fun printError(text: String) {
    logger.error("$ANSI_RED$text$ANSI_RESET")
}
