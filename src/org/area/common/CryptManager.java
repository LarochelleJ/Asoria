package org.area.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.StringEscapeUtils;
import org.area.game.GameServer;
import org.area.kernel.Main;
import org.area.object.Maps;
import org.area.object.Maps.Case;


public class CryptManager {


    public static String CryptPassword(String Key, String Password) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};

        String _Crypted = "#1";

        for (int i = 0; i < Password.length(); i++) {
            char PPass = Password.charAt(i);
            char PKey = Key.charAt(i);

            int APass = (int) PPass / 16;

            int AKey = (int) PPass % 16;

            int ANB = (APass + (int) PKey) % HASH.length;
            int ANB2 = (AKey + (int) PKey) % HASH.length;

            _Crypted += HASH[ANB];
            _Crypted += HASH[ANB2];
        }
        return _Crypted;
    }

    public static String decryptpass(String pass, String key) {
        int l1, l2, l3, l4, l5;
        String l7 = "";
        String Chaine = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
        for (l1 = 0; l1 <= (pass.length() - 1); l1 += 2) {
            l3 = (int) key.charAt((l1 / 2));
            l2 = Chaine.indexOf(pass.charAt(l1));
            l4 = (64 + l2) - l3;
            int l11 = l1 + 1;
            l2 = Chaine.indexOf(pass.charAt(l11));
            l5 = (64 + l2) - l3;
            if (l5 < 0) l5 = 64 + l5;

            l7 = l7 + (char) (16 * l4 + l5);
        }
        return l7;
    }

    public static String CryptIP(String IP) {
        String[] Splitted = IP.split("\\.");
        String Encrypted = "";
        int Count = 0;
        for (int i = 0; i < 50; i++) {
            for (int o = 0; o < 50; o++) {
                if (((i & 15) << 4 | o & 15) == Integer.parseInt(Splitted[Count])) {
                    Character A = (char) (i + 48);
                    Character B = (char) (o + 48);
                    Encrypted += A.toString() + B.toString();
                    i = 0;
                    o = 0;
                    Count++;
                    if (Count == 4)
                        return Encrypted;
                }
            }
        }
        return "DD";
    }

    public static String CryptPort(int config_game_port) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        int P = config_game_port;
        String nbr64 = "";
        for (int a = 2; a >= 0; a--) {
            nbr64 += HASH[(int) (P / (Math.pow(64, a)))];
            P = (int) (P % (int) (Math.pow(64, a)));
        }
        return nbr64;
    }

    public static String cellID_To_Code(int cellID) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};

        int char1 = cellID / 64, char2 = cellID % 64;
        return HASH[char1] + "" + HASH[char2];
    }

    public static int cellCode_To_ID(String cellCode) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        char char1 = cellCode.charAt(0), char2 = cellCode.charAt(1);
        int code1 = 0, code2 = 0, a = 0;
        while (a < HASH.length) {
            if (HASH[a] == char1) {
                code1 = a * 64;
            }
            if (HASH[a] == char2) {
                code2 = a;
            }
            a++;
        }
        return (code1 + code2);
    }

    public static int getIntByHashedValue(char c) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        for (int a = 0; a < HASH.length; a++) {
            if (HASH[a] == c) {
                return a;
            }
        }
        return -1;
    }

    public static char getHashedValueByInt(int c) {
        char[] HASH = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
        return HASH[c];
    }

    public static ArrayList<Case> parseStartCell(Maps map, int num) {
        ArrayList<Case> list = null;
        String infos = null;
        if (!map.get_placesStr().equalsIgnoreCase("-1")) {
            infos = map.get_placesStr().split("\\|")[num];
            int a = 0;
            list = new ArrayList<Case>();
            while (a < infos.length()) {
                list.add(map.getCase((getIntByHashedValue(infos.charAt(a)) << 6) + getIntByHashedValue(infos.charAt(a + 1))));
                a = a + 2;
            }
        }
        return list;
    }

    // @Flow - Version am�lior�e qui d�compile correctement les walkables cases
    public static Map<Integer, Case> DecompileMapData(Maps map,
                                                      String dData) {
        Map<Integer, Case> cells = new TreeMap<Integer, Case>();
        for (int f = 0; f < dData.length(); f += 10) {
            String CellData = dData.substring(f, f + 10);
            List<Byte> cellInfos = new ArrayList<Byte>();

            for (int i = 0; i < CellData.length(); i++)
                cellInfos.add((byte) getIntByHashedValue(CellData.charAt(i)));

            int walkable = ((cellInfos.get(2) & 56) >> 3);
            boolean los = (cellInfos.get(0) & 1) != 0;
            int layerObject2 = ((cellInfos.get(0) & 2) << 12)
                    + ((cellInfos.get(7) & 1) << 12) + (cellInfos.get(8) << 6)
                    + cellInfos.get(9);
            boolean layerObject2Interactive = ((cellInfos.get(7) & 2) >> 1) != 0;

            int object = (layerObject2Interactive ? layerObject2 : -1);
            cells.put(f / 10, new Case(map, f / 10, (walkable != 0
                    && !CellData.equalsIgnoreCase("bhGaeaaaaa") && !CellData.equalsIgnoreCase("Hhaaeaaaaa")), los, object));
        }
        return cells;
    }


    //Fonction qui convertis tout les textes ANSI(Unicode) en UTF-8. Les fichiers doivent �tre cod� en ANSI sinon les phrases seront illisible. Ansi et unicde c'est pas la même chose @Dumbass
    public static String toUtf(String _in) {
        String _out = "";

        try {
            _out = new String(_in.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("Conversion en UTF-8 echoue! : " + e.getMessage());
        }

        return _out;
    }

    //Utilis� pour convertir les inputs UTF-8 en String normal.
    public static String toUnicode(String _in) {
        String _out = "";

        try {
            _out = new String(_in.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Conversion en UNICODE! : " + e.getMessage());
        }

        return _out;
    }

    private static String[] HEX = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    public static String checksum(String s) {
        int a = 0;
        int b = 0;

        while (b < s.length()) {
            a += Character.codePointAt(s, b) % 16;
            b++;
        }
        return HEX[a % 16];
    }

    public static String decryptPacket(String packet) {
        String vraiPacket;
        String cleCryptage = "";
        String b = packet.substring(1, 2).toUpperCase();
        int b2 = 0;
        try {
            b2 = Integer.parseInt(b, 16);
        } catch (Exception e) {
            return "";
        }
        String packetPossible = decypherData(packet.substring(2), cleCryptage, b2 * 2);
        vraiPacket = packetPossible;
        /*if (checksum(packetPossible) != b) {
            vraiPacket = packet;
        } else {
            vraiPacket = packetPossible;
        }*/
        return vraiPacket;
    }

    public static String decypherData(String a, String b, int c) {
        String resultat = "";
        int longueurCle = b.length();
        int x = 0;
        int y = 0;

        while (y < a.length()) {
            resultat = resultat + Character.toString((char)(Integer.parseInt(a.substring(y, y + 2), 16) ^ b.charAt((x++ + c) % longueurCle)));
            y += 2;
        }
        return StringEscapeUtils.unescapeHtml(resultat);
    }

}
