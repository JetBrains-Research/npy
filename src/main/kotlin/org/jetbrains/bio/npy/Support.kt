package org.jetbrains.bio.npy

import java.util.*

/** A marker function for "impossible" `when` branches. */
@Suppress("nothing_to_inline")
internal inline fun impossible(): Nothing = throw IllegalStateException()

/** Returns a product of array values. */
@Suppress("nothing_to_inline")
internal inline fun IntArray.product() = this.reduce { a, b -> a * b}

/** This is a very VERY basic parser for repr of Python dict. */
internal fun parseDict(s: String): Map<String, Any> {
    return parseDictInternal(Lexer(s))
}

private fun parseDictInternal(lexer: Lexer): MutableMap<String, Any> {
    val acc = HashMap<String, Any>()
    lexer.eat(Token.LBRA)
    while (lexer.peekToken() !== Token.RBRA) {
        val key = lexer.nextToken(Token.LIT::class.java).data
        lexer.eat(Token.SEMI)
        val value: Any = lexer.peekToken().let {
            if (it is Token.LPAR) {
                parseTuple(lexer)
            } else {
                lexer.nextToken().data!!
            }
        }

        acc[key] = value
        lexer.tryEat(Token.COMMA)
    }

    lexer.eat(Token.RBRA)
    return acc
}

private fun parseTuple(lexer: Lexer): List<Int> {
    lexer.eat(Token.LPAR)
    val acc = ArrayList<Int>()
    while (lexer.peekToken() !== Token.RPAR) {
        val item = lexer.nextToken(Token.INT::class.java)
        acc.add(item.data)
        lexer.tryEat(Token.COMMA)
    }
    lexer.eat(Token.RPAR)

    // Returning an array makes the code harder to test because of
    // the array-array comparisons. Yes, I know.
    return acc
}

sealed class Token<out T>(val data: T) {
    override fun toString(): String = javaClass.simpleName

    object LBRA : Token<Char>('{')
    object RBRA : Token<Char>('}')
    object LPAR : Token<Char>('(')
    object RPAR : Token<Char>(')')
    object COMMA : Token<Char>(',')
    object SEMI : Token<Char>(':')

    object TRUE : Token<Boolean>(true)
    object FALSE : Token<Boolean>(false)
    class LIT(data: String) : Token<String>(data)
    class INT(data: Int) : Token<Int>(data)
}

class Lexer(private val s: CharSequence) {
    private var offset = 0
    private var peeked: Token<*>? = null

    @Suppress("unchecked_cast")
    fun <T> nextToken(type: Class<out Token<T>>): Token<T> {
        val token = nextToken()
        check(token.javaClass === type) {
            "Expected ${type.simpleName}, but got $token"
        }
        return token as Token<T>
    }

    fun nextToken(): Token<*> {
        if (peeked != null) {
            val token = peeked
            peeked = null
            return token!!
        }

        while (s[offset].isWhitespace()) {
            offset++
        }

        return when (s[offset++]) {
            '{' -> Token.LBRA
            '}' -> Token.RBRA
            '(' -> Token.LPAR
            ')' -> Token.RPAR
            ',' -> Token.COMMA
            ':' -> Token.SEMI
            '\'' -> {
                val tick = offset
                do {} while (s[offset++] != '\'')
                Token.LIT(s.substring(tick, offset - 1))
            }
            in '0'..'9' -> {
                val tick = offset - 1
                do {} while (!isEof() && s[offset++].isDigit())
                Token.INT(s.substring(tick, offset - 1).toInt())
            }
            else -> {
                val tick = offset - 1
                while (s[offset].isLetter()) {
                    offset++
                }

                val token = s.substring(tick, offset)
                when (token) {
                    "False" -> Token.FALSE
                    "True"  -> Token.TRUE
                    else    -> error(token)
                }
            }
        }
    }

    fun peekToken(): Token<*> {
        if (peeked == null) {
            peeked = nextToken()
        }

        return peeked!!
    }

    fun eat(expected: Token<*>) {
        val actual = nextToken()
        check(actual === expected) { "Expected $expected, got $actual" }
    }

    fun tryEat(expected: Token<*>) {
        if (!isEof() && peekToken() === expected) {
            nextToken()
        }
    }

    fun isEof() = offset == s.length
}