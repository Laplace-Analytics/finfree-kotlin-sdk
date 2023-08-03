package sdk.models

import java.util.*

class FinancialQuarter(val year: Int, month: Int) {

     val month = if (month > 3) {
         if (month > 6) {
             if (month > 9) {
                 12
             } else {
                 9
             }
         } else {
             6
         }
     } else {
         3
     }


    override fun equals(other: Any?): Boolean {
        if (other is FinancialQuarter) {
            return other.year == year && other.month == month
        }
        return false
    }

    fun getQuarter(): Int {
        return when {
            month <= 3 -> 1
            month <= 6 -> 2
            month <= 9 -> 3
            else -> 4
        }
    }

    val dateTime: Date
        get() {
            val calendar = Calendar.getInstance()
            calendar.set(year, month)
            return calendar.time
        }

    fun next(): FinancialQuarter {
        return if (month == 12) {
            FinancialQuarter(year + 1, 3)
        } else {
            FinancialQuarter(year, month + 3)
        }
    }

    fun previous(): FinancialQuarter {
        return if (month == 3) {
            FinancialQuarter(year - 1, 12)
        } else {
            FinancialQuarter(year, month - 3)
        }
    }

    fun interpolate(other: FinancialQuarter): List<FinancialQuarter> {
        val res = mutableListOf<FinancialQuarter>()

        val (curr, end) = if (isBefore(other)) {
            Pair(FinancialQuarter(year, month), FinancialQuarter(other.year, other.month))
        } else {
            Pair(FinancialQuarter(other.year, other.month), FinancialQuarter(year, month))
        }

        var currentQuarter = curr
        while (currentQuarter.isBefore(end)) {
            res.add(currentQuarter)
            currentQuarter = currentQuarter.next()
        }
        res.add(end)

        return res
    }

    fun isBefore(other: FinancialQuarter): Boolean {
        return other.year > year || (other.year == year && other.month > month)
    }

    override fun toString(): String {
        return "${year.toString().substring(2)}/$month"
    }

    companion object {
        fun fromDate(date: Date): FinancialQuarter {
            val calendar = Calendar.getInstance()
            calendar.time = date
            var year = calendar.get(Calendar.YEAR)
            var month = calendar.get(Calendar.MONTH)
            month = when{
                month > 9 -> 12
                month > 6 -> 9
                month > 3 -> 6
                else -> 3
            }
            return FinancialQuarter(year, month)
        }

        fun fromFormatOfYYYYQQ(date: String): FinancialQuarter {
            val year = date.substring(0, 4).toIntOrNull()
            val quarter = date.substring(5).toIntOrNull()
            if (year == null || quarter == null || quarter !in 1..4) {
                throw Exception("Invalid date format")
            }

            return FinancialQuarter(year, quarter * 3)
        }

        fun fromFormatOfYYYYMM(date: String): FinancialQuarter {
            val year = date.substring(0, 4).toIntOrNull()
            val month = date.substring(5).toIntOrNull()
            if (year == null || month == null || month !in 1..12) {
                throw Exception("Invalid date format")
            }

            return FinancialQuarter(year, month)
        }
    }



    override fun hashCode(): Int {
        return ("FY${toString()}").hashCode()
    }
}