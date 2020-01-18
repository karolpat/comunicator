package server;

import java.io.IOException;
import java.sql.*;

public class Db {
    public static Connection connection = null;

    public Db(String dbDriver, String dbUrl) throws IOException, SQLException, ClassNotFoundException {
        if(connection == null) {
            Class.forName(dbDriver);
            connection = DriverManager.getConnection(dbUrl);
            Statement st = connection.createStatement();
            // st.execute("DROP TABLE users");
            st.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, email VARCHAR(40), password VARCHAR(40), nick VARCHAR(20))");
            // st.execute("DROP TABLE messages");
            st.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER, sender INTEGER, recipient INTEGER, body VARCHAR(200))");
            // st.execute("DROP TABLE sessions");
            st.execute("CREATE TABLE IF NOT EXISTS sessions (session VARCHAR(36) PRIMARY KEY, created INTEGER, touched INTEGER, userid INTEGER, useridto INTEGER)");
            // st.execute("DROP TABLE membership");
            st.execute("CREATE TABLE IF NOT EXISTS membership (user_id INTEGER, group_id INTEGER, PRIMARY KEY(user_id, group_id) )");
            // st.execute("DROP TABLE group");
            st.execute("CREATE TABLE IF NOT EXISTS groups (group_id INTEGER PRIMARY KEY AUTOINCREMENT, group_name VARCHAR(40) )");
        }
    }
}
