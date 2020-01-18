package common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import server.Db;

public class Group {

	private int id;
	private String name;

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public int setId(int id) {
		this.id = id;
		return this.id;
	}

	public String setName(String name) {
		this.name = name;
		return this.name;
	}

	public Group(int id, String name) {
		this.setName(name);
		this.setId(id);
	}

	public static int createGroup(String name) {

		if (groupExists(name) == -1) {
			try {
				PreparedStatement prepSt = Db.connection.prepareStatement("INSERT INTO groups (group_name) VALUES (?)");
				prepSt.setString(1, name);
				prepSt.execute();
				ResultSet rs = prepSt.getGeneratedKeys();
				return rs.getInt(1);
			} catch (SQLException e) {
			}
		}

		return -1;
	}

	public static int groupExists(String name) {

		try {
			PreparedStatement prepSt = Db.connection.prepareStatement("SELECT * FROM groups WHERE group_name = ?");
			prepSt.setString(1, name);
			ResultSet rs = prepSt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
		}
		return -1;
	}

	public List<Integer> getGroupMembers(int id) {

		List<Integer> users = new ArrayList<Integer>();

		try {
			PreparedStatement prepSt = Db.connection
					.prepareStatement("SELECT user_id FROM membership WHERE group_id = ?");
			prepSt.setInt(1, id);
			ResultSet rs = prepSt.executeQuery();
			while (rs.next()) {
				users.add(rs.getInt(1));
			}

		} catch (SQLException e) {
		}

		return users;
	}

	public void closeGroup() {

		try {
			PreparedStatement prepSt = Db.connection.prepareStatement("DELETE FROM groups WHERE group_id = ?");
			prepSt.setInt(1, this.getId());
			prepSt.executeUpdate();
		} catch (SQLException e) {
		}

	}

}
