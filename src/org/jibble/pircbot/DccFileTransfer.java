package org.jibble.pircbot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DccFileTransfer {

    public static final int BUFFER_SIZE = 1024;
    DccFileTransfer(PircBot bot, DccManager manager, String nick, String login, String hostname, String type, String filename, long address, int port, long size) {
        _bot = bot;
        _manager = manager;
        _nick = nick;
        _login = login;
        _hostname = hostname;
        _type = type;
        _file = new File(filename);
        _address = address;
        _port = port;
        _size = size;
        _received = false;

        _incoming = true;
    }

    DccFileTransfer(PircBot bot, DccManager manager, File file, String nick, int timeout) {
        _bot = bot;
        _manager = manager;
        _nick = nick;
        _file = file;
        _size = file.length();
        _timeout = timeout;
        _received = true;

        _incoming = false;
    }

    public synchronized void receive(File file, boolean resume) {
        if (!_received) {
            _received = true;
            _file = file;

            if (_type.equals("SEND") && resume) {
                _progress = file.length();
                if (_progress == 0) {
                    doReceive(file, false);
                }
                else {
                    _bot.sendCTCPCommand(_nick, "DCC RESUME file.ext " + _port + " " + _progress);
                    _manager.addAwaitingResume(this);
                }
            }
            else {
                _progress = file.length();
                doReceive(file, resume);
            }
        }
    }

    void doReceive(final File file, final boolean resume) {
        new Thread() {
            public void run() {
                BufferedOutputStream foutput = null;
                Exception exception = null;
                try {
                    int[] ip = _bot.longToIp(_address);
                    String ipStr = ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
                    _socket = new Socket(ipStr, _port);
                    _socket.setSoTimeout(30*1000);
                    _startTime = System.currentTimeMillis();
                    _manager.removeAwaitingResume(DccFileTransfer.this);
                    BufferedInputStream input = new BufferedInputStream(_socket.getInputStream());
                    BufferedOutputStream output = new BufferedOutputStream(_socket.getOutputStream());
                    foutput = new BufferedOutputStream(new FileOutputStream(file.getCanonicalPath(), resume));
                    byte[] inBuffer = new byte[BUFFER_SIZE];
                    byte[] outBuffer = new byte[4];
                    int bytesRead = 0;
                    while ((bytesRead = input.read(inBuffer, 0, inBuffer.length)) != -1) {
                        foutput.write(inBuffer, 0, bytesRead);
                        _progress += bytesRead;
                        outBuffer[0] = (byte) ((_progress >> 24) & 0xff);
                        outBuffer[1] = (byte) ((_progress >> 16) & 0xff);
                        outBuffer[2] = (byte) ((_progress >> 8) & 0xff);
                        outBuffer[3] = (byte) ((_progress >> 0) & 0xff);
                        output.write(outBuffer);
                        output.flush();
                        delay();
                    }
                    foutput.flush();
                }
                catch (Exception e) {
                    exception = e;
                }
                finally {
                    try {
                        foutput.close();
                        _socket.close();
                    }
                    catch (Exception anye) {
                        // Do nothing.
                    }
                }

                _bot.onFileTransferFinished(DccFileTransfer.this, exception);
            }
        }.start();
    }

    void doSend(final boolean allowResume) {
        new Thread() {
            public void run() {

                BufferedInputStream finput = null;
                Exception exception = null;

                try {

                    ServerSocket ss = null;

                    int[] ports = _bot.getDccPorts();
                    if (ports == null) {
                        ss = new ServerSocket(0);
                    }
                    else {
                        for (int i = 0; i < ports.length; i++) {
                            try {
                                ss = new ServerSocket(ports[i]);
                                break;
                            }
                            catch (Exception e) {
                            }
                        }
                        if (ss == null) {
                            throw new IOException("All ports returned by getDccPorts() are in use.");
                        }
                    }

                    ss.setSoTimeout(_timeout);
                    _port = ss.getLocalPort();
                    InetAddress inetAddress = _bot.getDccInetAddress();
                    if (inetAddress == null) {
                        inetAddress = _bot.getInetAddress();
                    }
                    byte[] ip = inetAddress.getAddress();
                    long ipNum = _bot.ipToLong(ip);
                    String safeFilename = _file.getName().replace(' ', '_');
                    safeFilename = safeFilename.replace('\t', '_');

                    if (allowResume) {
                        _manager.addAwaitingResume(DccFileTransfer.this);
                    }
                    _bot.sendCTCPCommand(_nick, "DCC SEND " + safeFilename + " " + ipNum + " " + _port + " " + _file.length());
                    _socket = ss.accept();
                    _socket.setSoTimeout(30000);
                    _startTime = System.currentTimeMillis();
                    if (allowResume) {
                        _manager.removeAwaitingResume(DccFileTransfer.this);
                    }
                    ss.close();
                    BufferedOutputStream output = new BufferedOutputStream(_socket.getOutputStream());
                    BufferedInputStream input = new BufferedInputStream(_socket.getInputStream());
                    finput = new BufferedInputStream(new FileInputStream(_file));
                    if (_progress > 0) {
                        long bytesSkipped = 0;
                        while (bytesSkipped < _progress) {
                            bytesSkipped += finput.skip(_progress - bytesSkipped);
                        }
                    }
                    byte[] outBuffer = new byte[BUFFER_SIZE];
                    byte[] inBuffer = new byte[4];
                    int bytesRead = 0;
                    while ((bytesRead = finput.read(outBuffer, 0, outBuffer.length)) != -1) {
                        output.write(outBuffer, 0, bytesRead);
                        output.flush();
                        input.read(inBuffer, 0, inBuffer.length);
                        _progress += bytesRead;
                        delay();
                    }
                }
                catch (Exception e) {
                    exception = e;
                }
                finally {
                    try {
                        finput.close();
                        _socket.close();
                    }
                    catch (Exception e) {
                    }
                }

                _bot.onFileTransferFinished(DccFileTransfer.this, exception);
            }
        }.start();
    }

    void setProgress(long progress) {
        _progress = progress;
    }

    private void delay() {
        if (_packetDelay > 0) {
            try {
                Thread.sleep(_packetDelay);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public String getNick() {
        return _nick;
    }

    public String getLogin() {
        return _login;
    }

    public String getHostname() {
        return _hostname;
    }

    public File getFile() {
        return _file;
    }

    public int getPort() {
        return _port;
    }

    public boolean isIncoming() {
        return _incoming;
    }

    public boolean isOutgoing() {
        return !isIncoming();
    }

    public void setPacketDelay(long millis) {
        _packetDelay = millis;
    }

    public long getPacketDelay() {
        return _packetDelay;
    }

    public long getSize() {
        return _size;
    }

    public long getProgress() {
        return _progress;
    }

    public double getProgressPercentage() {
        return 100 * (getProgress() / (double) getSize());
    }

    public void close() {
        try {
            _socket.close();
        }
        catch (Exception e) {
        }
    }

    public long getTransferRate() {
        long time = (System.currentTimeMillis() - _startTime) / 1000;
        if (time <= 0) {
            return 0;
        }
        return getProgress() / time;
    }

    public long getNumericalAddress() {
        return _address;
    }
    private PircBot _bot;
    private DccManager _manager;
    private String _nick;
    private String _login = null;
    private String _hostname = null;
    private String _type;
    private long _address;
    private int _port;
    private long _size;
    private boolean _received;
    private Socket _socket = null;
    private long _progress = 0;
    private File _file = null;
    private int _timeout = 0;
    private boolean _incoming;
    private long _packetDelay = 0;
    private long _startTime = 0;
}