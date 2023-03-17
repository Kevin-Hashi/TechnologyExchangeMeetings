package jp.gr.java_conf.hashiguchi_homemade.robot_bt

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.E
import kotlin.math.pow

class MotorControl {
    private var lastMinus = false
    private val zero = BigDecimal.ZERO
    private val one = BigDecimal.ONE
    private val b90 = BigDecimal(90)
    private val b180 = BigDecimal(180)
    private val calcBase =
        { x: BigDecimal -> x.divide(BigDecimal(90), 10, RoundingMode.HALF_UP) }
    private val calc90to180 = { x: BigDecimal -> BigDecimal(2).subtract(calcBase(x)) }
    private val calcMinus90to180 = { x: BigDecimal -> calc90to180(x.abs()).negate() }
    private val checkX = { x: BigDecimal ->
        if (x.abs().compareTo(one) == 1) one.multiply(BigDecimal(x.signum())) else x
    }

    private fun BigDecimal.sqrt(scale: Int = 5): BigDecimal {
        var x = BigDecimal(kotlin.math.sqrt(this.toDouble()), MathContext.DECIMAL64)
        if (scale < 17) return x.setScale(scale, RoundingMode.HALF_UP)
        val b2 = BigDecimal(2)
        for (tempScale in 16 until scale step 2) {
            x = x.subtract(
                x.multiply(x).subtract(this).divide(x.multiply(b2), scale, RoundingMode.HALF_UP)
            )
        }
        return x.setScale(scale, RoundingMode.HALF_UP)
    }

    fun calcMotor(degree: BigDecimal): List<BigDecimal> {
        lastMinus = degree.compareTo(zero) == -1
        return when {
            degree.compareTo(zero) == 0 || degree.compareTo(zero) == 1 && degree.compareTo(b90) == -1 || degree.compareTo(
                b90
            ) == 0 -> listOf(one, calcBase(degree))
            degree.compareTo(b90) == 1 && degree.compareTo(b180) == -1 -> listOf(
                calc90to180(degree),
                one
            )
            degree.abs().compareTo(b180) == 0 && lastMinus -> listOf(zero, one.negate())
            degree.abs().compareTo(b180) == 0 && !lastMinus -> listOf(zero, one)
            degree.compareTo(b180.negate()) == 0 || degree.compareTo(b180.negate()) == 1 && degree.compareTo(
                b90.negate()
            ) == -1 -> listOf(calcMinus90to180(degree), one.negate())
            degree.compareTo(b90.negate()) == 0 || degree.compareTo(b90.negate()) == 1 && degree.compareTo(
                zero.negate()
            ) == -1 -> listOf(one.negate(), calcBase(degree))
            else -> listOf(BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    fun sgmLn99(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val p = BigDecimal.valueOf(3.0.pow(BigDecimal(4).multiply(checkedX).toDouble()))
        return BigDecimal(9).multiply(p.subtract(one))
            .divide(BigDecimal(8).multiply(p.add(BigDecimal(9))), 10, RoundingMode.HALF_UP)
    }

    fun sgmLn199(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val p = BigDecimal.valueOf(199.0.pow(BigDecimal(2).multiply(checkedX).toDouble()))
        return BigDecimal(199).multiply(p.subtract(one))
            .divide(BigDecimal(198).multiply(p.add(BigDecimal(199))), 10, RoundingMode.HALF_UP)
    }

    fun lSgmLn99(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val p = BigDecimal.valueOf(99.0.pow(checkedX.toDouble()))
        return BigDecimal(50).multiply(p.subtract(one))
            .divide(BigDecimal(49).multiply(p.add(one)), 10, RoundingMode.HALF_UP)
    }

    fun lSgmEP2(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val p = BigDecimal.valueOf(E.pow(BigDecimal.valueOf(E).pow(2).toDouble()))
        val px = BigDecimal.valueOf(p.toDouble().pow(checkedX.toDouble()))
        return (p.add(one)).multiply(px.subtract(one))
            .divide((p.subtract(one)).multiply(px.add(one)), 10, RoundingMode.HALF_UP)
    }

    fun rat1(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val sqrt5 = BigDecimal(5).sqrt(11)
        return checkedX.multiply(sqrt5.add(one)).divide(
            x.multiply(BigDecimal(2)).add(sqrt5.subtract(one)),
            10,
            RoundingMode.HALF_UP
        )
    }

    fun rat2(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val sqrt3 = BigDecimal(3).sqrt(11)
        return checkedX.multiply(sqrt3.add(one))
            .divide(
                x.multiply(BigDecimal(2)).add(sqrt3).subtract(one),
                10,
                RoundingMode.HALF_UP
            )
    }

    fun rat4(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val sqrt2 = BigDecimal(2).sqrt(11)
        return checkedX.multiply(sqrt2.add(one)).divide(
            (checkedX.multiply(BigDecimal(2))).add(sqrt2).subtract(one),
            10,
            RoundingMode.HALF_UP
        )
    }

    fun ratE(x: BigDecimal): BigDecimal {
        val checkedX = checkX(x)
        val sqrt4addE = (BigDecimal(4).add(BigDecimal.valueOf(E))).sqrt(11)
        val sqrtE = BigDecimal.valueOf(E).sqrt(11)
        return checkedX.multiply(sqrt4addE.add(sqrtE)).divide(
            checkedX.multiply(sqrtE).multiply(BigDecimal(2)).add(sqrt4addE).subtract(sqrtE),
            10,
            RoundingMode.HALF_UP
        )
    }
}