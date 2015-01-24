package com.dhruv.resumemanager;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Algorithms {
    private String input;

    Algorithms(String input) {
        this.input = input;
    }

    public String getMD5() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        md.update(input.getBytes());
        byte[] digested = md.digest();
        BigInteger bigInteger = new BigInteger(1, digested);
        String hashText = bigInteger.toString(16);
        while (hashText.length() < 32) {
            hashText = "0" + hashText;
        }

        return hashText;
    }
}
