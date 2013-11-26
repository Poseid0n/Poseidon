package irc.poseidon;

import java.util.Random;

public class NameGenerator {
    public static Random _r = new Random();
    public static char[] _alphabet = "abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWYZ1234567890_".toCharArray();
    public static String generateName() {
        String name = "";
        for(int k = 0; k < getLength(); k++) {
            for(int k1 = 0; k1 < _alphabet.length; k1++) {
                name = name + _alphabet[_r.nextInt(_alphabet.length)];
            }
        }
        return name;
    }

    public static int getLength() {
        int k = _r.nextInt(15);
        while(k < 4) {
            k = _r.nextInt(15);
        }
        return k;
    }
}