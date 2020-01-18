package server;

import common.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class Session {
    private String uuidStr;
    private int userid, useridTo;

    public Session(String uuidStr, int userid) {
        Session restored = null;
        if(uuidStr != null && !uuidStr.equals("")) {
            restored = new Session(uuidStr);
        }
        if(uuidStr == null || uuidStr.equals("") || (restored != null && restored.userid < 0)) {
            this.uuidStr = UUID.randomUUID().toString();
            this.userid = userid;
            // insert session data to DB
            int timestamp = Utils.now();
            try {
                PreparedStatement st = Db.connection.prepareStatement("INSERT INTO sessions (session,created,touched,userid,useridto) VALUES (?,?,?,?,0)");
                st.setString(1, this.uuidStr);
                st.setInt(2, timestamp);
                st.setInt(3, timestamp);
                st.setInt(4, this.userid);
                st.execute();
            } catch(SQLException ex) {};
        } else {
            this.uuidStr = uuidStr;
            this.userid = restored.userid;
            this.useridTo = restored.useridTo;
        }
    }

    public Session(String uuidStr) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT userid, useridto FROM sessions WHERE session=?");
            st.setString(1, uuidStr);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.userid = rs.getInt(1);
                this.useridTo = rs.getInt(2);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.uuidStr = null;
            this.userid = -1;
        }
    }

    public void update() {
        int timestamp = Utils.now();
        try {
            PreparedStatement st = Db.connection.prepareStatement("UPDATE sessions SET touched=? WHERE session=?");
            st.setInt(1, timestamp);
            st.setString(2, uuidStr);
            st.executeUpdate();
        } catch(SQLException ex) {}
    }

    public void update(int userid) {
        int timestamp = Utils.now();
        try {
            PreparedStatement st = Db.connection.prepareStatement("UPDATE sessions SET touched=?,userid=? WHERE session=?");
            st.setInt(1, timestamp);
            st.setInt(2, userid);
            st.setString(3, uuidStr);
            st.executeUpdate();
        } catch(SQLException ex) {}
    }

    public void update(int userid, int useridTo) {
        int timestamp = Utils.now();
        try {
            PreparedStatement st = Db.connection.prepareStatement("UPDATE sessions SET touched=?,userid=?,useridto=? WHERE session=?");
            st.setInt(1, timestamp);
            st.setInt(2, userid);
            st.setInt(3, useridTo);
            st.setString(4, uuidStr);
            st.executeUpdate();
        } catch(SQLException ex) {}
    }

    public int getUserId() {
        return userid;
    }

    public int getUserIdTo() { return useridTo; }

    public String toString() {
        return uuidStr;
    }
}