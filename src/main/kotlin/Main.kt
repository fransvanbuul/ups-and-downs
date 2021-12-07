import java.lang.IllegalArgumentException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/* Basic vector algebra. */
data class Vec(val x: Double, val z: Double) {
    fun norm(): Double = sqrt(x*x + z*z)
    operator fun plus(other: Vec) = Vec(this.x + other.x, this.z + other.z)
    operator fun minus(other: Vec) = Vec(this.x - other.x, this.z - other.z)
    operator fun times(scale: Double) = Vec(this.x * scale, this.z * scale)
    operator fun div(scale: Double) = Vec(this.x / scale, this.z / scale)
    operator fun times(other: Vec) = this.x * other.x + this.z * other.z
    operator fun unaryMinus() = Vec(-this.x, -this.z)
    fun asUnit() = this.div(this.norm())
    fun rotateLeft90() = Vec(-this.z, this.x)
}

/* Physical constants. */
const val m: Double = 0.1 // mass of the ball in kg
const val mu: Double = 0.00 // friction coefficient, dimensionless
const val L: Double = 1.0 // distance between points A and B in m
const val g: Double = 9.81 // gravitational acceleration in m/s^2
val vI: Vec = Vec(1.0, 0.0) // initial velocity, m/s

/* Simulation parameters. */
const val dx: Double = 0.000001 // negligable distance, in meters
const val dt: Double = 0.0000001 // timestep in seconds

/* Our model of the shape of the trajectory. */
open class Shape(val h: (Double) -> Double) {
    init {
        if(abs(h(0.0)) > dx) throw IllegalArgumentException("h doesn't start at A")
        if(abs(h(L)) > dx) throw IllegalArgumentException("h doesn't end at B")
    }
    operator fun invoke(x: Double): Double = h(x)
    fun normal(x: Double) = Vec(dx, h(x+0.5*dx) - h(x-0.5*dx)).asUnit().rotateLeft90()
}

/* Calculating all the forces given shape and position */
fun force(shape: Shape, x: Double): Vec {
    val fGravity = Vec(0.0, -m*g)
    val n = shape.normal(x)
    val fNormal = -n*(fGravity*n)
    val fFriction = n.rotateLeft90()*mu*fNormal.norm()
    return fGravity + fNormal + fFriction
}

/* Find out how long the ball takes to reach the target by numerical integration. */
fun tFinal(shape: Shape): Double {
    var x = Vec(0.0, 0.0)
    var v = vI
    var t = 0.0

    while( x.x < L ) {
        x += v * dt
        val a = force(shape, x.x)/m
        v += a * dt
        if(v.x < 0) throw IllegalStateException("v.x < 0")
        t += dt
    }

    return t
}

/* Some ideas on shapes */
object Straight : Shape({ _ -> 0.0 }) {
    override fun toString() = "Straight"
}
class Cosine(val n: Int, val a: Double) : Shape({ x -> a * (cos(2.0 * PI * n * x / L) - 1.0)}) {
    override fun toString() = "Cosine(#periods = ${"%4d".format(n)}, amplitude = ${"%.4f".format(a)})"
}

fun calculate(shape: Shape) {
    println("t_f($shape) = ${"%.6f".format(tFinal(shape))} s")
}

fun main() {
    println("m = ${m} kg, L = ${L} m, mu = ${mu}, vI = ${vI} m/s, g = ${g} m/s²")
    println()
    calculate(Straight)
    println()
    for(n in 1..9) calculate(Cosine(n, 0.050))
    println()
    for(n in 1..9) calculate(Cosine(n, 0.005))
    println()
    for(a in 2 .. 20) calculate(Cosine(1, 0.025 * a))
    println()
    for(n in 1..9) calculate(Cosine(n*1000, 0.0025))
}
