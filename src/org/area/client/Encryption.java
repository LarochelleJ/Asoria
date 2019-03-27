package org.area.client;

import org.area.kernel.Config;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Permet de crypter et décrypter les packets dofus envoyés avec le packet AKXYY
 * X = une valeur entre 0-9 ou A-F
 * YY = la valeur X en hexadecimal
 */

public class Encryption {

    public String key = "0";
    private String preparedKey = "0";
    private String HEX_CHARS[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    public Encryption(String key) {
        if (Config.USE_KEY){
            this.key = key;
            this.preparedKey = prepareKey(key);
        }
    }

    public String prepareKey(String key) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < key.length(); i += 2)
            sb.append((char) Integer.parseInt(key.substring(i, i + 2), 16));

        try {
            return URLDecoder.decode(sb.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public boolean isActive(){
        return key != "0";
    }

    public String prepareData(String message) {
        StringBuilder str = new StringBuilder();
        // Append keyId
        str.append(HEX_CHARS[1]);
        // Append checksum
        int checksum = checksum(message);
        str.append(HEX_CHARS[checksum]);
        // Prepare key cause it's hexa form
        int c = checksum * 2;
        String data = encode(message);
        int keyLength = preparedKey.length();

        for (int i = 0; i < data.length(); i++)
            str.append(decimalToHexadecimal(data.charAt(i) ^ preparedKey.charAt((i + c) % keyLength)));

        return str.toString();
    }

    public String unprepareData(String message) {
        int c = Integer.parseInt(Character.toString(message.charAt(1)), 16) * 2;
        StringBuilder str = new StringBuilder();
        int j = 0, keyLength = preparedKey.length();

        for(int i = 2; i < message.length(); i = i + 2)
            str.append((char) (Integer.parseInt(message.substring(i, i + 2), 16) ^ preparedKey.charAt((j++ + c) % keyLength)));

        try {
            String data = str.toString();
            data = data.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            data = data.replaceAll("\\+", "%2B");
            return URLDecoder.decode(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private int checksum(String data) {
        int result = 0;
        for(char c : data.toCharArray())
            result += c % 16;
        return result % 16;
    }

    private boolean isUnsafe(char ch) {
        return ch > 255 || "+%".indexOf(ch) >= 0;
    }

    private char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private String decimalToHexadecimal(int c) {
        if(c > 255) c = 255;
        return HEX_CHARS[c / 16] + "" + HEX_CHARS[c % 16];
    }
}