package common;

import server.Db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class Message {
    private int id, sender, recipient;
    private int timestamp;
    private String body;

    public Message(int sender, int recipient, String body) {
        this.id = -1;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = Utils.now();
        this.body = body;
        try {
            PreparedStatement st = Db.connection.prepareStatement("INSERT INTO messages (timestamp,sender,recipient,body) VALUES (?,?,?,?)");
            st.setInt(1, timestamp);
            st.setInt(2, sender);
            st.setInt(3, recipient);
            st.setString(4, body);
            st.execute();
            ResultSet rs = st.getGeneratedKeys();
            this.id = rs.getInt(1);
        } catch(SQLException ex) {};
    }

    public Message(int id) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT timestamp,sender,recipient,body FROM messages WHERE id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = id;
                this.timestamp = rs.getInt(1);
                this.sender = rs.getInt(2);
                this.recipient = rs.getInt(3);
                this.body = rs.getString(4);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
        }
    }

    public static int getMessagesCount() {
        int result = -1;
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM messages");
            rs.next();
            result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    public static int getMessagesCount(int recipient) {
        int result = -1;
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT COUNT(*) FROM messages WHERE recipient=?");
            st.setInt(1, recipient);
            ResultSet rs = st.executeQuery();
            rs.next();
            result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    public static int[] getIds(int recipient, int stampFrom, int stampTo) {
        int n = getMessagesCount(recipient);
        int[] result = new int[n];
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id FROM messages WHERE recipient=? AND timestamp>=? AND timestamp<=?");
            st.setInt(1, recipient);
            st.setInt(2, stampFrom);
            st.setInt(3, stampTo);
            ResultSet rs = st.executeQuery();
            n = 0;
            while(rs.next()) {
                result[n] = rs.getInt(1);
                n++;
            }
        } catch(SQLException ex) {}
        return result;
    }

    public static int[] getIds(int recipient, int stampFrom) {
        return getIds(recipient, stampFrom, Integer.MAX_VALUE);
    }

    public static int[] getIds(int recipient) {
        return getIds(recipient, 0, Integer.MAX_VALUE);
    }

    public String toString() {
        return Utils.stampToString(timestamp) + " " + new User(sender).toString() + " -> " + new User(recipient).toString() + ": " + body;
    }
}
