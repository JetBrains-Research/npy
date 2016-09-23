package org.jetbrains.bio.npy

import java.util.*

/** This is a very VERY basic parser for repr of Python dict. */
internal fun parseDict(s: String): Map<String, Any> {
    return parseDictInternal(tokenize(s))
}

private fun parseDictInternal(lexer: PeekingIterator<SpannedToken>): MutableMap<String, Any> {
    val acc = HashMap<String, Any>()
    lexer.eat(Token.LBRA)
    while (lexer.peek().token !== Token.RBRA) {
        val key = lexer.eat(Token.STR).span.removeSurrounding("\'")
        lexer.eat(Token.SEMI)
        val value: Any = lexer.peek().let {
            if (it.token == Token.LPAR) {
                parseTuple(lexer)
            } else {
                val st = lexer.next()
                when (st.token) {
                    Token.TRUE  -> true
                    Token.FALSE -> false
                    Token.INT   -> st.span.toInt()
                    Token.STR   -> st.span.removeSurrounding("\'")
                    else         -> error("Unexpected token: ${st.token}")
                }
            }
        }

        acc[key] = value
        lexer.tryEat(Token.COMMA)
    }

    lexer.eat(Token.RBRA)
    return acc
}

private fun parseTuple(lexer: PeekingIterator<SpannedToken>): List<Int> {
    lexer.eat(Token.LPAR)
    val acc = ArrayList<Int>()
    while (lexer.peek().token != Token.RPAR) {
        val item = lexer.eat(Token.INT)
        acc.add(item.span.toInt())
        lexer.tryEat(Token.COMMA)
    }
    lexer.eat(Token.RPAR)

    // Returning an array makes the code harder to test because of
    // the array-array comparisons. Yes, I know.
    return acc
}

private enum class Token(pattern: String) {
    LBRA("\\{"), RBRA("\\}"),
    LPAR("\\("), RPAR("\\)"),
    COMMA(","),
    SEMI(":"),

    TRUE("True"), FALSE("False"),
    INT("\\d+"),
    STR("'[^']+'");

    val regex = "^$pattern".toRegex()
}

private data class SpannedToken(val token: Token, val span: String)

/** Greedily tokenize a string for [parseDict]. */
private fun tokenize(s: String): PeekingIterator<SpannedToken> {
    var leftover = s
    val tokens = generateSequence {
        val best = Token.values().mapNotNull { token ->
            token.regex.find(leftover)?.let { SpannedToken(token, it.value) }
        }.maxBy { it.span.length }

        if (best != null) {
            leftover = leftover.substring(best.span.length)
                    .dropWhile(Char::isWhitespace)
        }

        best
    }

    return PeekingIterator(tokens.iterator())
}

/** Consume a token and return it. */
private fun Iterator<SpannedToken>.eat(expected: Token): SpannedToken {
    val st = next()
    check(st.token == expected) { "Expected $expected, but got ${st.token}" }
    return st
}

/** Try to consume a token and return it or `null` if anything goes wrong. */
private fun PeekingIterator<SpannedToken>.tryEat(expected: Token): SpannedToken? {
    return if (hasNext() && peek().token == expected) {
        next()
    } else {
        null
    }
}

// XXX doesn't support iterators which might yield null.
internal class PeekingIterator<out T>(private val it: Iterator<T>): Iterator<T> {
    private var hasPeeked = false
    private var item: T? = null

    override fun hasNext() = hasPeeked || it.hasNext()

    override fun next(): T {
        return if (hasPeeked) {
            val result = item
            hasPeeked = false
            item = null
            result!!
        } else {
            it.next()
        }
    }

    fun peek(): T {
        if (!hasPeeked) {
            item = it.next()
            hasPeeked = true
        }

        return item!!
    }
}
