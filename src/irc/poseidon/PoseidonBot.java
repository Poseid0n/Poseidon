package irc.poseidon;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

public class PoseidonBot extends PircBot implements Runnable {

    private ProxyObject _o;
    private String _server;
    private int _port;
    private int retryTimes = -1;
    private Poseidon _p;

    public PoseidonBot(String server, int port, String nick, ProxyObject o, Poseidon p) {
        _p = p;
        _o = o;
        _server = server;
        _port = port;
        setVerbose(true);
        setName(nick);
        setLogin(nick);
        setVersion(nick);
    }

    public void onDisconnect() {
        retryTimes++;
        if (retryTimes == 0) {
            try {
                reconnect();
            } catch (IOException | IrcException ex) {
                Poseidon.botList.remove(this);
            }
        } else {
            Poseidon.botList.remove(this);
            Poseidon.proxyList.remove(_o);
            _p.list2.remove(this.getName());
            this.dispose();
        }
    }

    @Override
    public void run() {
        try {
            connect(_server, _port, _o);
        } catch (IOException ex) {
            Logger.getLogger(PoseidonBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IrcException ex) {
            Logger.getLogger(PoseidonBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
