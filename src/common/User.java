package common;

import server.Db;
import sun.rmi.runtime.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class User {
    private int id;
    private String email;
    private String nick;

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNick() {
        return nick;
    }

    public User(String email, String password, String nick) {
        this.id = -1;
        this.email = email;
        this.nick = nick;
        try {
            User uniq = new User(nick);
            if(uniq.id > 0) throw new SQLException();
            PreparedStatement st = Db.connection.prepareStatement("INSERT INTO users (email,password,nick) VALUES (?,?,?)");
            st.setString(1, email);
            st.setString(2, password);
            st.setString(3, nick);
            st.execute();
            ResultSet rs = st.getGeneratedKeys();
            this.id = rs.getInt(1);
        } catch(SQLException ex) {};
    }

    public User(int id) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,nick FROM users WHERE id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.nick = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.nick = null;
        }
    }

    public User(String nick) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,nick FROM users WHERE nick=?");
            st.setString(1, nick);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.nick = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.nick = null;
        }
    }

    public User(String nick, String password) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT id,email,nick FROM users WHERE nick=? AND password=?");
            st.setString(1, nick);
            st.setString(2, password);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                this.id = rs.getInt(1);
                this.email = rs.getString(2);
                this.nick = rs.getString(3);
            } else throw new SQLException();
        } catch(SQLException ex) {
            this.id = -1;
            this.email = null;
            this.nick = null;
        }
    }

    public static int getUsersCount() {
        int result = -1;
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            result = rs.getInt(1);
        } catch(SQLException ex) {}
        return result;
    }

    public static int[] getUsersIds() {
        int n = getUsersCount();
        int[] result = new int[n];
        try {
            Statement st = Db.connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT id FROM users");
            n = 0;
            while(rs.next()) {
                result[n] = rs.getInt(1);
                n++;
            }
        } catch(SQLException ex) {}
        return result;
    }
    
    public int leaveGroup(int groupId) {
    	
    	try {
			PreparedStatement prepSt = Db.connection.prepareStatement("DELETE FROM membership WHERE user_id = ? AND group_id = ?");
			prepSt.setInt(1, this.getId());
			prepSt.setInt(2, groupId);
			return prepSt.executeUpdate();
			
		} catch (SQLException e) {}
    	
    	return -1;
    	
    }

    public boolean isGroup() {
        return email == null || email.length() == 0;
    }

    public int getGroupByName(String name) {
        int[] myGroups = getGroups();
        if(myGroups == null) return -1;
        for(int group_id: myGroups) { // browse only my groups not all
            User group = new User(group_id);
            if(group.id > 0 && group.nick.equals(name)) return group.id;
        }
        return -1;
    }

    public int createGroup(String name) {
        int existing = getGroupByName(name);
        if(existing > 0) return -1; // cannot create group of the same name as one I belong to
        User group = new User("", "", name);
        if(group.id > 0) { // adding myself to just created group
            try {
                PreparedStatement st = Db.connection.prepareStatement("INSERT INTO membership (user_id,group_id) VALUES (?,?)");
                st.setInt(1, id);
                st.setInt(2, group.id);
                st.executeUpdate();
            } catch(SQLException ex) {}
        }
        return group.id;
    }

    public static boolean isMember(int user_id, int group_id) {
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT * FROM membership WHERE user_id=? AND group_id=?");
            st.setInt(1, user_id);
            st.setInt(2, group_id);
            return st.executeQuery().next();
        } catch(SQLException ex) {}
        return false;
    }

    public static boolean addToGroup(int user_id, int group_id) {
        if(user_id <= 0) return false;
        try {
            PreparedStatement st = Db.connection.prepareStatement("INSERT INTO membership (user_id,group_id) VALUES (?,?)");
            st.setInt(1, user_id);
            st.setInt(2, group_id);
            st.executeUpdate();
            return true;
        } catch(SQLException ex) {}
        return false;
    }

    public int[] getMembers(int group_id) {
        if(!isMember(id, group_id)) return null; // cannot ask for members of group which I do not belong to
        int count = -1;
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT COUNT(*) FROM membership WHERE group_id=?");
            st.setInt(1, group_id);
            ResultSet rs = st.executeQuery();
            rs.next();
            count = rs.getInt(1);
        } catch(SQLException ex) {}
        if(count < 0) return null;
        int[] result = new int[count];
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT user_id FROM membership WHERE group_id=?");
            st.setInt(1, group_id);
            ResultSet rs = st.executeQuery();
            int n = 0;
            while(rs.next()) {
                result[n] = rs.getInt(1);
                n++;
            }
        } catch(SQLException ex) {}
        return result;
    }

    public int[] getGroups() { // which I belong to
        int count = -1;
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT COUNT(*) FROM membership WHERE user_id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            rs.next();
            count = rs.getInt(1);
        } catch(SQLException ex) {}
        if(count < 0) return null;
        int[] result = new int[count];
        try {
            PreparedStatement st = Db.connection.prepareStatement("SELECT group_id FROM membership WHERE user_id=?");
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            int n = 0;
            while(rs.next()) {
                result[n] = rs.getInt(1);
                n++;
            }
        } catch(SQLException ex) {}
        return result;
    }

    @Override
    public String toString() {
        return nick + "<" + email + ">";
    }
}