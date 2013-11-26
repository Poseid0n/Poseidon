package irc.poseidon;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

public class ProxyObject {

    private String _ip = "";
    private int _port = -1;
    private String _portAsString = "";

    public ProxyObject(String ip, int port) {
        _ip = ip;
        _port = port;
        _portAsString = port + "";
    }

    public String getIp() {
        return _ip;
    }

    public int getPort() {
        return _port;
    }

    public String getPortAsString() {
        return _portAsString;
    }

    public Proxy getProxyFrom() {
        SocketAddress addr = new InetSocketAddress(getIp(), getPort());
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
        return proxy;
    }
}
