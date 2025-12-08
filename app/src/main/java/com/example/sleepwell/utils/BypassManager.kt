package com.example.sleepwell.utils

import kotlin.random.Random

/**
 * Enum representing the type of bypass method
 */
enum class BypassMethod {
    NONE,           // Strict mode - no bypass allowed
    MATH,           // Math problem challenge
    STRING_MATCH    // Random string match challenge
}

/**
 * Data class representing a math challenge
 */
data class MathChallenge(
    val question: String,
    val answer: Int
)

/**
 * Utility class for generating and validating bypass challenges
 */
object BypassManager {

    /**
     * Generate a medium-difficulty math problem
     * Includes:
     * - Two-digit addition (e.g., 23 + 17)
     * - Two-digit subtraction (e.g., 45 - 18)
     * - Single-digit multiplication (e.g., 7 × 8)
     * - Simple division with whole number results (e.g., 24 ÷ 6)
     */
    fun generateMathChallenge(): MathChallenge {
        val operations = listOf(
            Operation.ADDITION,
            Operation.SUBTRACTION,
            Operation.MULTIPLICATION,
            Operation.DIVISION
        )

        val operation = operations.random()

        return when (operation) {
            Operation.ADDITION -> {
                val num1 = Random.nextInt(10, 50)
                val num2 = Random.nextInt(10, 50)
                MathChallenge(
                    question = "$num1 + $num2 = ?",
                    answer = num1 + num2
                )
            }

            Operation.SUBTRACTION -> {
                val num1 = Random.nextInt(20, 60)
                val num2 = Random.nextInt(10, num1) // Ensure positive result
                MathChallenge(
                    question = "$num1 - $num2 = ?",
                    answer = num1 - num2
                )
            }

            Operation.MULTIPLICATION -> {
                val num1 = Random.nextInt(6, 13)
                val num2 = Random.nextInt(6, 13)
                MathChallenge(
                    question = "$num1 × $num2 = ?",
                    answer = num1 * num2
                )
            }

            Operation.DIVISION -> {
                val divisor = Random.nextInt(4, 13)
                val multiplier = Random.nextInt(3, 11)
                val dividend = divisor * multiplier
                MathChallenge(
                    question = "$dividend ÷ $divisor = ?",
                    answer = dividend / divisor
                )
            }
        }
    }

    /**
     * Validate the user's answer to a math challenge
     */
    fun validateMathAnswer(challenge: MathChallenge, userAnswer: String): Boolean {
        return try {
            val answer = userAnswer.trim().toIntOrNull() ?: return false
            answer == challenge.answer
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate a random 8-character alphanumeric string
     * Characters: A-Z, 0-9 (excluding easily confused characters like O, 0, I, l, 1)
     */
    fun generateRandomString(length: Int = 8): String {
        // Exclude confusing characters: O, 0, I, l, 1
        val chars = ('A'..'Z') + ('a'..'z') + ('2'..'9')
        val filteredChars = chars.filter {
            it !in listOf('O', 'I', 'l')
        }

        return (1..length)
            .map { filteredChars.random() }
            .joinToString("")
    }

    /**
     * Validate the user's input against the target string
     * Case-sensitive exact match
     */
    fun validateStringMatch(target: String, userInput: String): Boolean {
        return target == userInput
    }

    /**
     * Convert bypass method enum to display string
     */
    fun getBypassMethodDisplayName(method: BypassMethod): String {
        return when (method) {
            BypassMethod.NONE -> "None (Strict Mode)"
            BypassMethod.MATH -> "Math Problem"
            BypassMethod.STRING_MATCH -> "String Match"
        }
    }

    /**
     * Get description for bypass method
     */
    fun getBypassMethodDescription(method: BypassMethod): String {
        return when (method) {
            BypassMethod.NONE -> "No bypass allowed. Apps stay locked until the timer expires."
            BypassMethod.MATH -> "Solve a math problem to unlock the app early."
            BypassMethod.STRING_MATCH -> "Type a random string correctly to unlock the app early."
        }
    }

    private enum class Operation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION
    }
}
