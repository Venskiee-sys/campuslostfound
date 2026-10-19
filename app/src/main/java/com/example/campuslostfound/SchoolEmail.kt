package com.example.campuslostfound

import java.util.Locale

/**
 * The sign-up / sign-in policy for UniFind: every account must use a
 * Philippine school address — an email whose domain ends in `.edu.ph`, such as
 * `juan@plmun.edu.ph`. Personal accounts (`@gmail.com`, `@yahoo.com`, …) are
 * rejected in the forms, after the Google account picker, and (as a hard
 * backstop that a modified APK cannot skip) by the `before-user-created` hook
 * in `supabase/restrict_edu_ph.sql`.
 */
object SchoolEmail {

    /** Every allowed address ends with this: `local@<school>.edu.ph`. */
    const val ALLOWED_SUFFIX = ".edu.ph"

    /** Shown wherever an address fails the check, on forms and from Google. */
    const val REQUIREMENT_MESSAGE =
        "Please use your school email ending in .edu.ph (e.g. juan@plmun.edu.ph)."

    /** True when [email] looks like `local@<school>.edu.ph` (case-insensitive). */
    fun isAllowed(email: String): Boolean {
        val domain = email.trim().substringAfterLast('@', "").lowercase(Locale.ROOT)
        return domain == "edu.ph" || domain.endsWith(ALLOWED_SUFFIX)
    }
}
