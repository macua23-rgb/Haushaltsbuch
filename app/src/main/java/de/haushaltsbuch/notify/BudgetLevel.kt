package de.haushaltsbuch.notify

/** 0 = alles gut, 80 = ab 80 % verbraucht, 100 = Budget aufgebraucht. */
fun alertLevel(spent: Long, limit: Long): Int = when {
    limit <= 0 -> 0
    spent >= limit -> 100
    spent * 100 >= limit * 80 -> 80
    else -> 0
}
