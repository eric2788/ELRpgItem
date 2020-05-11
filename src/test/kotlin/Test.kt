import kotlin.random.Random

fun main() {
    var t = 0
    var f = 0
    for (i in 1..0){
        if (Random.nextDouble() > 0.5) t++ else f++
    }

    println("true: $t")
    println("false: $f")
}