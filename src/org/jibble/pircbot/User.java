package org.jibble.pircbot;

public class User {

    User(String prefix, String nick) {
        _prefix = prefix;
        _nick = nick;
        _lowerNick = nick.toLowerCase();
    }

    public String getPrefix() {
        return _prefix;
    }

    public boolean isOp() {
        return _prefix.indexOf('@') >= 0;
    }

    public boolean hasVoice() {
        return _prefix.indexOf('+') >= 0;
    }

    public String getNick() {
        return _nick;
    }

    public String toString() {
        return this.getPrefix() + this.getNick();
    }

    public boolean equals(String nick) {
        return nick.toLowerCase().equals(_lowerNick);
    }

    public boolean equals(Object o) {
        if (o instanceof User) {
            User other = (User) o;
            return other._lowerNick.equals(_lowerNick);
        }
        return false;
    }

    public int hashCode() {
        return _lowerNick.hashCode();
    }

    public int compareTo(Object o) {
        if (o instanceof User) {
            User other = (User) o;
            return other._lowerNick.compareTo(_lowerNick);
        }
        return -1;
    }
    private String _prefix;
    private String _nick;
    private String _lowerNick;
}