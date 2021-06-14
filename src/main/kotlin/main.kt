import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main(args: Array<String>) {
    println("Hi there! Who are you? ")
    val user = (readLine() ?: "unknown").trim().lowercase()
    val deck = getDeck(user)

    val dueToday = deck.dueToday().size
    println()

    if (dueToday == 0) {
        println("All done for the day! \uD83C\uDF89")
        return
    } else {
        println("You have $dueToday cards due today.")
    }

    println()

    val newCards = deck.runDeck()

    println("All done! \uD83D\uDCA9")

    deck.copy(cards = newCards).save()
}

@ExperimentalTime
fun getDeck(user: String): Deck {
    val file = File("$user.json")

    if (file.exists()) {
        return Json.decodeFromString(file.readText())
    }

    return Deck(createCards(), user)
}

@ExperimentalTime
fun createCards(): List<Card> {
    val cards = mutableListOf<Card>()

    IntRange(2, 12).forEach { i ->
        IntRange(2, 12).forEach { j ->
            cards.add(Card(
                display = "$i × $j = ",
                answer = (i * j).toString()
            ))
        }
    }

    return cards
}

@Serializable
@ExperimentalTime
data class Deck(private val cards: List<Card>, private val user: String) {
    fun dueToday(): List<Card> {
        return cards.filter { it.dueToday() }.take(10)
    }

    fun save() {
        File("$user.json").writeText(Json {prettyPrint = true}.encodeToString(this))
    }

    fun runDeck(): List<Card> {
        val discards = mutableListOf<Card>()

        dueToday().forEach { card ->
            print(card.display)
            val answer = readLine()
            println()

            if (card.isAnswer(answer)) {
                print("Got it! \uD83E\uDD73")
                discards.add(card.known())
                Thread.sleep(1000)
            } else {
                print("Oops. It's actually ${card.answer}.")
                discards.add(card.unKnown())
                Thread.sleep(3000)
            }

            println()
            println()
        }

        return cards.map { card ->
            discards.find { it.display == card.display } ?: card.unKnown()
        }
    }
}

@ExperimentalTime
@Serializable
data class Card(
    val display: String,
    val answer: String,
    val step: Int = 24,

    @Contextual
    val nextUp: Instant? = null
) {
    fun dueToday() = nextUp == null || nextUp < now()

    fun known(): Card {
        val nextStep = step * 2
        return copy(step = nextStep, nextUp = now() + hours(nextStep - 2))
    }

    fun unKnown(): Card {
        val nextStep = max(24, step / 2)
        return copy(step = nextStep, nextUp = now() + hours(nextStep - 2))
    }

    fun isAnswer(candidate: String?): Boolean {
        return candidate != null && candidate.trim() == answer
    }
}
