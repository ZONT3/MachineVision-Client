package ru.zont.mvc;
import java.util.Objects;
import java.util.Random;

@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
class RandomString {

    /**
     * Generate a random string.
     */
    String nextString() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

    private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    //private static final String lower = upper.toLowerCase(Locale.ROOT);

    private static final String digits = "0123456789";

    private static final String alphanum = upper + digits;

    private final Random random;

    private final char[] symbols;

    private final char[] buf;

    RandomString(int length, Random random, String symbols) {
        if (length < 1) throw new IllegalArgumentException();
        if (symbols.length() < 2) throw new IllegalArgumentException();
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }

    /**
     * Create an alphanumeric string generator.
     */
    @SuppressWarnings("SameParameterValue")
    RandomString(int length, Random random) {
        this(length, random, alphanum);
    }

}
