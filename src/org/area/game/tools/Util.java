package org.area.game.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.area.client.Account;
import org.area.common.SQLManager;

import com.mysql.jdbc.PreparedStatement;

public class Util {
	
	public static int loadPointsByAccount(Account account) {
			
			int points = 0;
			try{
				ResultSet RS = SQLManager.executeQuery("SELECT points FROM accounts WHERE guid = '"+account.getGuid()+"';", true);
				while(RS.next()){
					if (RS.getInt("points") <= 0)
						points = 0;
					else
						points = RS.getInt("points");
				}
				RS.getStatement().close();
				RS.close();
			}catch (Exception e){e.printStackTrace();}
			
			return points;
	}
	
	public static void updatePointsByAccount(Account account, int diff) {
		
		String Request = "UPDATE accounts SET points = '"+diff+"' WHERE guid = '"+account.getGuid()+"';";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection(true));
			PS.execute();
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static String getActualDate() { //Leur calendar pourrit faut vraiment l'améliorer u_u 
		
		int year = Calendar.getInstance().get(Calendar.YEAR);
		String mounth = String.valueOf((Calendar.getInstance().get(Calendar.MONTH)+1));
		String dayMounth = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		String hours = String.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		String minutes = String.valueOf(Calendar.getInstance().get(+Calendar.MINUTE));
		String seconds = String.valueOf(Calendar.getInstance().get(Calendar.SECOND));
		int mounthM = Integer.parseInt(mounth); int dayMounthM = Integer.parseInt(dayMounth), hoursM = Integer.parseInt(hours), 
			minutesM = Integer.parseInt(minutes), secondsM =Integer.parseInt(seconds);
		
		if (mounthM == 13) mounth = "1"; if (mounthM < 10) mounth = 0 + mounth;
		if (dayMounthM < 10) dayMounth = 0 + dayMounth; if (hoursM < 10) hours = 0 + hours;
		if (minutesM < 10) minutes = 0 + minutes; if (secondsM < 10) seconds = 0 + seconds;
		
		return "[" + dayMounth + "/" + mounth + "/" + year + "] " + hours + ":" + minutes + ":" + seconds;
	}
	
}
