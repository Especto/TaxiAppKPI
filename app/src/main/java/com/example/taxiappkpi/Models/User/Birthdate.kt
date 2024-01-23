package com.example.taxiappkpi.Models.User

data class Birthdate(
    var year: Int = 0,
    var month: Int = 0,
    var day: Int = 0
) {
    companion object {
        fun strToBirthdate(date: String): Birthdate? {
            val regexPattern = Regex("""^\d{4}-\d{2}-\d{2}$""")

            if (!regexPattern.matches(date)) {
                return null
            }

            val components = date.split("-")
            if (components.size != 3) {
                return null
            }

            val year = components[0].toInt()
            val month = components[1].toInt()
            val day = components[2].toInt()

            if (year < 0 || month < 1 || month > 12 || day < 1 || day > 31) {
                return null
            }

            return Birthdate(year, month, day)
        }

        fun birthdateToStr(date: Birthdate): String {
            return "${date.year}-${addNull(date.month)}-${addNull(date.day)}"
        }

        private fun addNull(number: Int):String {
            return if(number<10){
                "0$number"
            } else{
                "$number"
            }

        }
    }
}
