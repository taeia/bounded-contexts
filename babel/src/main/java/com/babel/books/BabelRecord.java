package com.babel.books;

import java.util.List;

/**
 * What Babel knows about one ISBN, in Babel's shape. The names and the conventions are
 * Babel's: the number is thirteen digits with no hyphens; the title and the subtitle
 * are two fields, and the subtitle is null when there is none; the people are
 * "contributors", surname first, with a role in brackets after the name when they are
 * not the author; and there is a year and a publisher, which Babel has and the library
 * does not want. None of this is wrong. It is simply not the library's model of a
 * book, and the library did not choose it.
 */
public record BabelRecord(String isbn13, String title, String subtitle, List<String> contributors,
                          int year, String publisher) {
}
