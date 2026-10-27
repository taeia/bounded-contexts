package library.catalogue.model;

/**
 * An International Standard Book Number, the identity of a book in the catalogue. A
 * value object: thirteen digits with a check digit at the end, which this class
 * checks, so that no Isbn exists that a publisher could not have issued. It is kept as
 * the cataloguer wrote it, with or without hyphens, because the hyphens are how
 * people read it; two spellings of the same number are the same Isbn, so equality and
 * hashing look at the digits alone. The lending context carries the same thirteen
 * digits as a String it never reads, which is all it needs: the number is the one
 * thing the two contexts agree on about a book.
 */
public record Isbn(String text) {

    public Isbn {
        if (text == null || !digitsOf(text).matches("\\d{13}") || !checksumHolds(digitsOf(text))) {
            throw new CatalogueException(text + " is not an ISBN.");
        }
    }

    public static Isbn of(String text) {
        return new Isbn(text);
    }

    /**
     * The thirteen digits, with the hyphens and spaces taken out.
     */
    public String digits() {
        return digitsOf(text);
    }

    private static String digitsOf(String text) {
        return text.replaceAll("[-\\s]", "");
    }

    /**
     * ISBN-13: the digits are weighted 1, 3, 1, 3, ... and the sum must be a
     * multiple of ten.
     */
    private static boolean checksumHolds(String digits) {
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = digits.charAt(i) - '0';
            sum += i % 2 == 0 ? digit : 3 * digit;
        }
        return sum % 10 == 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Isbn isbn && digits().equals(isbn.digits());
    }

    @Override
    public int hashCode() {
        return digits().hashCode();
    }

    @Override
    public String toString() {
        return text;
    }
}
