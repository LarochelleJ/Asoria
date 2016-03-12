package org.area.exchange;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.area.kernel.Config;
import org.area.kernel.Console;
import org.area.kernel.Console.Color;
import org.area.kernel.Main;

public class ExchangeHandler extends IoHandlerAdapter {
	
	@Override
    public void sessionCreated(IoSession arg0) throws Exception {
		Console.println("connection oppened", Color.GREEN);
    }
	
    @Override
    public void messageReceived(IoSession arg0, Object arg1) throws Exception {
    	String packet = ioBufferToString(arg1);
    	if (Config.DEBUG)
    		Console.println("<-- " + packet, Color.GREEN);
        
        ExchangePacketHandler.parser(packet, arg0);
    }
    
    @Override
    public void messageSent(IoSession arg0, Object arg1) throws Exception {
    	String packet = ioBufferToString(arg1);
    	if (Config.DEBUG)
    		Console.println("--> " + packet, Color.GREEN);
    }
    
    @Override
    public void sessionClosed(IoSession arg0) throws Exception {
    	Console.println("connection closed", Color.GREEN);
    	Console.println("connection lost with the login server", Color.RED);
    	
    	Main.exchangeClient.restart();
    }
    
    @Override
    public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
    	Console.println("connection exception : ", Color.GREEN);
    	arg1.printStackTrace();
    }
    
	public static String ioBufferToString(Object o) {
    	IoBuffer ioBuffer = IoBuffer.allocate(2048);
    	ioBuffer.put((IoBuffer) o);
    	ioBuffer.flip();
    		
    	CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
    	
    	try { return ioBuffer.getString(charsetDecoder);
		} catch (CharacterCodingException e) { }
    	return "undefined";
	}
}
