package org.area.kernel;

public class SlowBase64 {

    /**
     * The first 64 digits in a number system with base=64digits,
     * for Base64 encoding
     */
    private static final String radixBase64="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=יטא9ךפצגהןüû~ס";

    public static String encode(String string){
        String whole_binary = "";
        for(char c:string.toCharArray()){
            String char_to_binary = Integer.toBinaryString(c);
            while(char_to_binary.length()<8)
                char_to_binary="0"+char_to_binary;
            whole_binary+=char_to_binary;
        }
        string="";
        String suffix="";
        
        for(int i=0;i<whole_binary.length();i+=6){
            String six_binary_digits = null;
            try{
                six_binary_digits = whole_binary.substring(i, i+6);
            }
            catch(StringIndexOutOfBoundsException sioobe){
                six_binary_digits = whole_binary.substring(i);
                while(six_binary_digits.length()<6){
                    six_binary_digits+="00";
                    suffix+="=";
                }
            }
            string+=radixBase64.charAt(Integer.parseInt(six_binary_digits,2));
        }
        return string+suffix;
    }

    public static String decode(String string){
        String binary_string="";
        for(char c:string.toCharArray()){
            if(c=='=')
                break;
            String char_to_binary = Integer.toBinaryString(radixBase64.indexOf(c));
            while(char_to_binary.length()<6)
                char_to_binary="0"+char_to_binary;
            binary_string+=char_to_binary;
        }
        if(string.endsWith("=="))
            binary_string=binary_string.substring(0, binary_string.length()-4);
        else if(string.endsWith("="))
            binary_string=binary_string.substring(0, binary_string.length()-2);
        string="";
        for(int i=0;i<binary_string.length();i+=8){
            String eight_binary_digits = binary_string.substring(i, i+8);
            string+=(char)Integer.parseInt(eight_binary_digits,2);
        }
        return string;
    }
    
    public static void main(String[] args) {
        for(String arg:args){
            System.out.println("Input   String: "+arg);
            System.out.println("Encoded String: "+(arg=encode(arg)));
            System.out.println("Decoded String: "+(arg=decode(arg)));
            System.out.println("----------------------------------");
        }
    }

}