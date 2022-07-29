package com.nacl.secondkill.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class MD5Util {
    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    private static final String salt="nacl202228";

    public static String inputPassToFromPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(1) + inputPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    public static String formPassToDBPass(String formPass, String salt) {
        String str = salt.charAt(0) + salt.charAt(1) + formPass + salt.charAt(2) + salt.charAt(3);
        return md5(str);
    }

    public static String inputPassDBPass(String inputPass, String salt) {
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass, salt);
        return dbPass;
    }

    public static void main(String[] args) {
        //f3fb136af3a54839ca8ab9c0edfac513
        System.out.println(inputPassToFromPass("123456"));
        System.out.println(formPassToDBPass("4115930bc5ac36ad1c3af0d47abd00d8",salt));
        System.out.println(inputPassDBPass("123456", salt));
    }
}
