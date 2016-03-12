package org.area.kernel;

import java.io.IOException;
import java.util.Calendar;

public class Logs {
	public static boolean isUsed = false;
	
	public static void addToDebug(String str)
	{
		if(!isUsed)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Config.Log_Debug.write("["+date+"] "+str);
			Config.Log_Debug.newLine();
			Config.Log_Debug.flush();
		} catch (IOException e) {}
	}
	public static void addToMjLog(String str)
	{
		if(!isUsed)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Config.Log_MJ.write(str+"  ["+date+"]");
			Config.Log_MJ.newLine();
			Config.Log_MJ.flush();
		} catch (IOException e) {}
	}
	
	public static void addToShopLog(String str)
	{
		if(!isUsed)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Config.Log_Shop.write("["+date+"]"+str);
			Config.Log_Shop.newLine();
			Config.Log_Shop.flush();
		} catch (IOException e) {}
	}
	public static void addToFmLog(String str)
	{
		if(!isUsed)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Config.Log_FM.write("["+date+"]"+str);
			Config.Log_FM.newLine();
			Config.Log_FM.flush();
		} catch (IOException e) {}
	}

	public static void addToChatLog(String str)
	{
		if(!isUsed)return;
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)+":"+Calendar.getInstance().get(+Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND);
		try {
			Config.Log_Chat.write("["+date+"]"+str);
			Config.Log_Chat.newLine();
			Config.Log_Chat.flush();
		} catch (IOException e) {}
	}
}
