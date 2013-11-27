package irc.poseidon;

import java.util.Random;

public class NameGenerator {

    public static Random _r = new Random();
    public static char[] _alphabet = "abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWYZ1234567890".toCharArray();

    public static String generateName() {
        String name = "";
        for (int k = 0; k < getLength(); k++) {
            name = name + _alphabet[_r.nextInt(_alphabet.length)];
        }
        return name;
    }

    public static int getLength() {
        int k = _r.nextInt(15);
        while (k < 4) {
            k = _r.nextInt(15);
        }
        return k;
    }
}
