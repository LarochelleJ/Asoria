package org.area.exchange;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.area.game.GameServer;
import org.area.kernel.Console;
import org.area.kernel.Console.Color;
import org.area.kernel.Main;

public class ExchangeClient {

    public static int port, i;
    public static String ip;
    public static IoSession myActiveSession;
    private Timer timer = new Timer();
    public boolean pong;
    private boolean ping = false;

    private IoConnector ioConnector = new NioSocketConnector();
    public ConnectFuture connectFuture;

    public ExchangeClient() {
        ioConnector.setHandler(new ExchangeHandler());
    }

    public boolean start() {
        GameServer.state = 0;
        System.out.println("aaa?");
        connectFuture = ioConnector.connect(new InetSocketAddress(ip, port));
        //connectFuture.awaitUninterruptibly(); // On attend que la connecton soit établie
        System.out.println("dd?");
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }

        if (!ioConnector.isActive()) {
            if (!Main.isRunning) return false;

            Console.println("try to connect...", Color.RED);

            restart();
            return ioConnector.isActive();
        }
        verifConnection();
        Console.println("exchange client connected", Color.GREEN);
        return !ioConnector.isActive();
    }

    public void verifConnection() {
        TimerTask myTask = new TimerTask() {
            @Override
            public void run() {
                if (myActiveSession != null) {
                    if (!ping) {
                        try {
                            Main.exchangeClient.send("PING", myActiveSession);
                            Console.println("- PING SENT -");
                        } catch (Exception e) {
                        }
                        ping = true;
                        pong = false;
                    } else {
                        if (!pong) {
                            restart();
                        }
                        ping = false;
                    }
                }
            }
        };
        timer.schedule(myTask, 10000, 10000);
    }

    public void restart() {
        if (!Main.isRunning) return;

        Console.println("login server not found", Color.RED);
        ioConnector.dispose();
        connectFuture.cancel();
        timer.cancel();
        //ioConnector = new NioSocketConnector();
        Main.exchangeClient = new ExchangeClient();

        while (Main.exchangeClient.start()) ;
    }

    public void stop() {
        ioConnector.dispose();
        connectFuture.cancel();
        Console.println("exchange server stopped", Color.RED);
    }

    void send(String packet, IoSession ioSession) {
        ioSession.write(StringToIoBuffer(packet));
    }

    public static IoBuffer StringToIoBuffer(String s) {
        IoBuffer ioBuffer = IoBuffer.allocate(2048);
        ioBuffer.put(s.getBytes());

        return ioBuffer.flip();
    }
}
