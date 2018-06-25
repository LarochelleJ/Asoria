package org.area.kernel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;

import org.area.common.Constant;
import org.area.common.SQLManager;
import org.area.exchange.ExchangeClient;
import org.area.game.GameServer;

public class Config {

	/** Utils **/
	public static PrintStream PS;
	public static boolean DEBUG = false;
	public static boolean LOGS;
	public static String CONFIG_LOG_MJ_FLOUET = "";
	static BufferedReader config;
	public static boolean BETA = false; // Indique si version beta ou offi

	/** Database **/
	public static String DB_NAME, DB_HOST, DB_USER, DB_PASS;
	public static String RDB_NAME, RDB_HOST, RDB_USER, RDB_PASS;
	public static int CONFIG_REALM_PORT = 443, CONFIG_GAME_PORT = 5555,
			CONFIG_DB_COMMIT = 1000;

	/** GameServer **/
	public static boolean CONFIG_USE_IP = false;
	public static String GAMESERVER_IP;
	public static boolean IS_SAVING = false;
	public static int CONFIG_PLAYER_LIMIT = 200;
	public static boolean CONFIG_ALLOW_MULTI = false;
	public static boolean CONFIG_POLICY = false;

	/** Logs **/
	public static BufferedWriter Log_GameSock;
	public static BufferedWriter Log_Hdv;
	public static BufferedWriter Log_Game;
	public static BufferedWriter Log_Realm;
	public static BufferedWriter Log_IpCheck;
	public static BufferedWriter Log_MJ;
	public static BufferedWriter Log_RealmSock;
	public static BufferedWriter Log_Shop;
	public static BufferedWriter Log_Debug;
	public static BufferedWriter Log_Chat;
	public static BufferedWriter Log_FM;
	
	/** Kolizeum **/
	public static String KOLIMAPS;
	
	/** Guerre de guilde **/
	public static int GVG_XP_PLAYER = 0;
	public static int GVG_XP_GUILDE = 0;
	public static int GVG_GAIN_TICKET = 0;
	public static int GVG_GAIN_JETON = 0;
	
	/** DeathMatch **/
	public static int DMMAX_PLAYER = 3;
    public static int DM_LEVEL = 10;
    
	/** Timers & Times **/
	public static Timer fmTimer = new Timer();
	public static Timer fightTimer = new Timer();
	public static final int FIGHT_START_TIME = 55000;
	public static int CONFIG_ARENA_TIMER = 10 * 60 * 1000;
	
	/** Rates **/
	public static int RATE_PVM = 1, RATE_KAMAS = 1, RATE_HONOR = 1;
	public static int RATE_METIER = 1, RATE_FM = 1, RATE_DROP = 1;
	public static int RATE_XP_PVP = 10;

	/** Config the Server **/
	public static int START_LEVEL = 1;
	public static int START_KAMAS = 0;
	public static short START_MAP = 951;
	public static int START_CELL = 156;

	public static int MAX_PLAYERS_PER_ACCOUNT = 5;
	public static String CONFIG_MOTD = "B30016";
	public static String CONFIG_MOTD_COLOR = "";

	public static boolean CONFIG_USE_MOBS = false;
	public static boolean AURA_SYSTEM = false;
	public static boolean ALLOW_MULE_PVP = false;
	public static boolean CONFIG_ZAAP = false;
	public static long FLOOD_TIME = 30000;
	
	public static ArrayList<Integer> NOTINHDV = new ArrayList<Integer>();
	public static int CONFIG_SECONDS_FOR_BONUS = 3600;
	public static int CONFIG_BONUS_MAX = 100;
	public static String CONFIG_SORT_INDEBUFFABLE = new String();
	public static ArrayList<Integer> CONFIG_MORPH_PROHIBIDEN = new ArrayList<Integer>();
	public static int COINS_ID;
	public static int RATE_COINS;
	public static String FORMULE_TACLE = "50,1,50,1,50,50,100,100";
	public static String TACLE_POURCENTAGE = "0,50,50,80,100";
	
	
	/** Maps - Other **/
	public static short CONFIG_MAP_JAIL = 8534;
	public static int CONFIG_CELL_JAIL = 297;
	public static String RESTRICTED_MAPS = "";
	public static ArrayList<Integer> ARENA_MAPS = new ArrayList<Integer>(8);
	
	/** Pubs **/
	public static String PUB1 = "", PUB2 = "", PUB3 = "", PUB4 = "", PUB5 = "";
	
	/** Ornement **/
	public static boolean ORNEMENT = false;
	public static String ORNEMENT_EXCLUS = "";

	// Fonctionnalité Meneur Suiveur
	public static boolean MENEUR_SUIVEUR_ACTIVE = true;

	/** Prestiges **/
	public static int MAX_PRESTIGES = 15;
	
	/** Traques **/
	public static int CONFIG_TRAQUE_DIFFERENCE = 15;
	
	/** ShopSocket System **/
	public static String WEB_IP = "176.31.46.218";
	public static boolean ALLOW_NEUTRE_PVP;

	public static boolean load() {
		try {
			config = new BufferedReader(new FileReader("config.txt"));
			
			String line = "";

			while ((line = config.readLine()) != null) {

				if (line.split("=").length == 1)
					continue;
				String param = line.split("=")[0].trim();
				String value = line.split("=")[1].trim();

				if (param.equalsIgnoreCase("DEBUG")
						&& value.equalsIgnoreCase("true"))
					DEBUG = true;

				else if (param.equalsIgnoreCase("SEND_POLICY")
						&& value.equalsIgnoreCase("true"))
					CONFIG_POLICY = true;
				
				else if (param.equalsIgnoreCase("LOGS") 
						&& value.equalsIgnoreCase("true")) {
					LOGS = true;
					Logs.isUsed = true;
				}
				
				
				else if (param.equalsIgnoreCase("BONUS_MAX")) {
					CONFIG_BONUS_MAX = Integer.parseInt(value);

					if (CONFIG_BONUS_MAX < 0)
						CONFIG_BONUS_MAX = 0;
					if (CONFIG_BONUS_MAX > 1000)
						CONFIG_BONUS_MAX = 1000;
				}
				else if (param.equalsIgnoreCase("START_KAMAS")) {

					START_KAMAS = Integer.parseInt(value);

					if (START_KAMAS < 0)
						START_KAMAS = 0;
					if (START_KAMAS > 1000000000)
						START_KAMAS = 1000000000;
				} else if (param.equalsIgnoreCase("START_LEVEL")) {
					START_LEVEL = Integer.parseInt(value);

					if (START_LEVEL < 1)
						START_LEVEL = 1;
					if (START_LEVEL > 200)
						START_LEVEL = 200;

				} else if (param.equalsIgnoreCase("START_MAP"))
					START_MAP = Short.parseShort(value);

				else if (param.equalsIgnoreCase("START_CELL"))
					START_CELL = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_KAMAS"))
					RATE_KAMAS = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_HONOR"))
					RATE_HONOR = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_PVM"))
					RATE_PVM = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_XP_PVP"))
					RATE_XP_PVP = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_DROP"))
					RATE_DROP = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("ZAAP")
						&& value.equalsIgnoreCase("true"))
					CONFIG_ZAAP = true;
				
				else if (param.equalsIgnoreCase("ALLOW_NEUTRE_PVP")
						&& value.equalsIgnoreCase("true"))
					ALLOW_NEUTRE_PVP = true;

				else if (param.equalsIgnoreCase("USE_IP")
						&& value.equalsIgnoreCase("true"))
					CONFIG_USE_IP = true;

				else if (param.equalsIgnoreCase("LOG_FLOUET"))
					CONFIG_LOG_MJ_FLOUET = line.split("=", 2)[1].trim();

				else if (param.equalsIgnoreCase("MOTD"))
					CONFIG_MOTD = line.split("=", 2)[1];

				else if (param.equalsIgnoreCase("RESTRICTED_MAPS"))
					RESTRICTED_MAPS = line.split("=", 2)[1].trim();

				else if (param.equalsIgnoreCase("MOTD_COLOR"))
					CONFIG_MOTD_COLOR = value;

				else if (param.equalsIgnoreCase("RATE_JOB"))
					RATE_METIER = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("GAME_ID"))
					GameServer.id = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("GAME_KEY"))
					GameServer.key = value;

				else if (param.equalsIgnoreCase("EXCHANGE_PORT"))
					ExchangeClient.port = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("EXCHANGE_IP"))
					ExchangeClient.ip = value;
				else if (param.equalsIgnoreCase("GAME_PORT"))
					CONFIG_GAME_PORT = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("REALM_PORT"))
					CONFIG_REALM_PORT = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("FLOODER_TIME"))
					FLOOD_TIME = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("RATE_COINS"))
					RATE_COINS = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("WEB_IP"))
					WEB_IP = value;

				else if (param.equalsIgnoreCase("RATE_FM"))
					RATE_FM = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("FORMULE_TACLE"))
					FORMULE_TACLE = value;

				else if (param.equalsIgnoreCase("TACLE_POURCENTAGE"))
					TACLE_POURCENTAGE = value;

				else if (param.equalsIgnoreCase("HOST_IP"))
					GameServer.setIp(value);

				else if (param.equalsIgnoreCase("COINS_ID"))
					COINS_ID = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("GAME_HOST"))
					DB_HOST = value;

				else if (param.equalsIgnoreCase("GAME_USER"))
					DB_USER = value;

				else if (param.equalsIgnoreCase("GAME_PASS"))
					DB_PASS = value == null ? "" : value;

				else if (param.equalsIgnoreCase("DB_NAME"))
					DB_NAME = value;

				else if (param.equalsIgnoreCase("REALM_HOST"))
					RDB_HOST = value;
				
				else if (param.equalsIgnoreCase("LANG"))
					GameServer.lang = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("REALM_USER"))
					RDB_USER = value;

				else if (param.equalsIgnoreCase("REALM_PASS"))
					RDB_PASS = value == null ? "" : value;

				else if (param.equalsIgnoreCase("REALM_DB_NAME"))
					RDB_NAME = value;

				else if (param.equalsIgnoreCase("MAX_PERSO_PAR_COMPTE"))
					MAX_PLAYERS_PER_ACCOUNT = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("USE_MOBS"))
					CONFIG_USE_MOBS = value.equalsIgnoreCase("true");

				else if (param.equalsIgnoreCase("ALLOW_MULTI_ACCOUNT"))
					CONFIG_ALLOW_MULTI = value.equalsIgnoreCase("true");

				else if (param.equalsIgnoreCase("PLAYER_LIMIT"))
					CONFIG_PLAYER_LIMIT = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("ARENA_MAP"))
					for (String curID : value.split(","))
						ARENA_MAPS.add(Integer.parseInt(curID));

				else if (param.equalsIgnoreCase("ARENA_TIMER"))
					CONFIG_ARENA_TIMER = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("KOLIMAPS"))
					KOLIMAPS = value;
				
				else if (param.equalsIgnoreCase("AURA_SYSTEM"))
					AURA_SYSTEM = value.equalsIgnoreCase("true");

				else if (param.equalsIgnoreCase("ALLOW_MULE_PVP"))
					ALLOW_MULE_PVP = value.equalsIgnoreCase("true");

				else if (param.equalsIgnoreCase("NOT_IN_HDV"))
					for (String curID : value.split(","))
						NOTINHDV.add(Integer.parseInt(curID));
				
				else if (param.equalsIgnoreCase("GVG_XP_PLAYER"))
					GVG_XP_PLAYER = Integer.parseInt(value);
				
				else if (param.equalsIgnoreCase("GVG_XP_GUILDE"))
					GVG_XP_GUILDE = Integer.parseInt(value);
				
				else if (param.equalsIgnoreCase("GVG_GAIN_TICKET"))
					GVG_GAIN_TICKET = Integer.parseInt(value);
				
				else if (param.equalsIgnoreCase("GVG_GAIN_JETON"))
					GVG_GAIN_JETON = Integer.parseInt(value);
				
				else if (param.equalsIgnoreCase("PUB1"))
					PUB1 = value;

				else if (param.equalsIgnoreCase("PUB2"))
					PUB2 = value;

				else if (param.equalsIgnoreCase("PUB3"))
					PUB3 = value;
				
				else if (param.equalsIgnoreCase("PUB4"))
					PUB4 = value;
				
				else if (param.equalsIgnoreCase("PUB5"))
					PUB5 = value;
				else if (param.equalsIgnoreCase("ORNEMENT"))
					ORNEMENT = value.equalsIgnoreCase("true");
				else if (param.equalsIgnoreCase("ORNEMENT_EXCLUS"))
					ORNEMENT_EXCLUS = value;
				else if (param.equalsIgnoreCase("MAX_PRESTIGES"))
					MAX_PRESTIGES = Integer.parseInt(value);
				else if (param.equalsIgnoreCase("CELL_JAIL"))
					CONFIG_CELL_JAIL = Integer.parseInt(value);

				else if (param.equalsIgnoreCase("CONFIG_SORT_INDEBUFFABLE"))
					CONFIG_SORT_INDEBUFFABLE = value;
				else if(param.equalsIgnoreCase("CONFIG_MORPH_PROHIBIDEN"))
				{
					for(String split : value.split("\\|"))
					{
						String[] infos = split.split(":");
						CONFIG_MORPH_PROHIBIDEN.add(Integer.parseInt(infos[0]));
					}
				}
			}

			if (DB_NAME == null || DB_HOST == null || DB_PASS == null
					|| DB_USER == null)
				throw new Exception();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out
					.println("config.properties file not found or unreadable");
			Reboot.reboot();
			return false;
		}

		if (DEBUG)
			Constant.DEBUG_MAP_LIMIT = 20000;

		try {
			String date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
					+ "-" + (Calendar.getInstance().get(Calendar.MONTH) + 1)
					+ "-" + Calendar.getInstance().get(Calendar.YEAR);

			if (Logs.isUsed) {
				Log_GameSock = new BufferedWriter(new FileWriter(
						"logs/Game_logs/" + date + "_packets.txt", true));
				Log_Hdv = new BufferedWriter(new FileWriter("logs/Hdv_logs/"
						+ date + ".txt", true));
				Log_Game = new BufferedWriter(new FileWriter("logs/Game_logs/"
						+ date + ".txt", true));
				Log_Realm = new BufferedWriter(new FileWriter(
						"logs/Realm_logs/" + date + ".txt", true));
				Log_IpCheck = new BufferedWriter(new FileWriter(
						"logs/IpCheck_logs/" + date + ".txt", true));
				Log_RealmSock = new BufferedWriter(new FileWriter(
						"logs/Realm_logs/" + date + "_packets.txt", true));
				Log_Shop = new BufferedWriter(new FileWriter("logs/Shop_logs/"
						+ date + ".txt", true));
				Log_FM = new BufferedWriter(new FileWriter("logs/FM_logs/"
						+ date + ".txt", true));

				if (CONFIG_LOG_MJ_FLOUET.isEmpty()) {
					Log_MJ = new BufferedWriter(new FileWriter("logs/Gms_logs/"
							+ date + "_GM.txt", true));
					Log_Debug = new BufferedWriter(new FileWriter(
							"logs/Thread_logs/" + date + "_Thread.txt", true));
					Log_Chat = new BufferedWriter(new FileWriter(
							"logs/Chat_logs/" + date + "_Chat.txt", true));
					String nom = "logs/Error_logs/" + date + "_error.txt";
					int i = 0;
					while (new File(nom).exists()) {
						nom = "logs/Error_logs/" + date + "_error_" + i
								+ ".txt";
						i++;
					}
					PS = new PrintStream(new File(nom));
				} else {
					Log_MJ = new BufferedWriter(new FileWriter(
							CONFIG_LOG_MJ_FLOUET + "/logs/Gms_logs/" + date
									+ "_GM.txt", true));
					Log_Debug = new BufferedWriter(new FileWriter(
							CONFIG_LOG_MJ_FLOUET + "/logs/Thread_logs/" + date
									+ "_Thread.txt", true));
					Log_Chat = new BufferedWriter(new FileWriter(
							CONFIG_LOG_MJ_FLOUET + "/logs/Chat_logs/" + date
									+ "_Chat.txt", true));

					String nom = CONFIG_LOG_MJ_FLOUET + "/logs/Error_logs/"
							+ date + "_error.txt";
					int i = 0;
					while (new File(nom).exists()) {
						nom = CONFIG_LOG_MJ_FLOUET + "/logs/Error_logs/" + date
								+ "_error_" + i + ".txt";
						i++;
					}
					PS = new PrintStream(new File(nom));
				}
				LOGS = true;
				System.setErr(PS);
				PS.append("server starting..\n");
				PS.flush();
				String str = "server starting...\n";
				Log_GameSock.write(str);
				Log_Hdv.write(str);
				Log_Game.write(str);
				Log_MJ.write(str);
				Log_Realm.write(str);
				Log_IpCheck.write(str);
				Log_RealmSock.write(str);
				Log_Shop.write(str);
				Log_FM.write(str);
				Log_Debug.write(str);
				Log_Chat.write(str);
				Log_Debug.newLine();
				Log_Debug.flush();
				Log_GameSock.flush();
				Log_Hdv.flush();
				Log_Game.flush();
				Log_MJ.flush();
				Log_Realm.flush();
				Log_IpCheck.flush();
				Log_RealmSock.flush();
				Log_Shop.flush();
				Log_Chat.flush();
			}
		} catch (IOException e) {
			/* On créer les dossiers */
			System.out.println("create the logs file");
			new File("logs").mkdir();
			new File("logs/Shop_logs").mkdir();
			new File("logs/Game_logs").mkdir();
			new File("logs/Hdv_logs").mkdir();
			new File("logs/Realm_logs").mkdir();
			new File("logs/Error_logs").mkdir();
			new File("logs/IpCheck_logs").mkdir();
			new File("logs/FM_logs").mkdir();

			if (CONFIG_LOG_MJ_FLOUET.isEmpty()) {
				new File("logs/Thread_logs").mkdir();
				new File("logs/Gms_logs").mkdir();
				new File("logs/Chat_logs").mkdir();
			} else {
				new File(CONFIG_LOG_MJ_FLOUET + "/logs/Thread_logs").mkdir();
				new File(CONFIG_LOG_MJ_FLOUET + "/logs/Gms_logs").mkdir();
				new File(CONFIG_LOG_MJ_FLOUET + "/logs/Chat_logs").mkdir();
			}

			System.out.println(e.getMessage());
			load();
			return false;
		}
		if (Main.isRunning) { // La connexion bdd est déjà actve
			SQLManager.LOAD_CONFIG();
		}
		return true;
	}
}
