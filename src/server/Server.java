/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.List;
import javax.swing.*;

import common.Group;
import common.Message;
import common.User;

/**
 * @author Mariusz
 */
public class Server implements Runnable {

	/**
	 * @param args
	 *            the command line arguments
	 * @throws java.io.IOException
	 */
	private static int port;
	private static JLabel nThreadsLabel, nRegisteredUsers, nMessages;
	private static JTextArea logWindow;
	private static JScrollPane scrollPane;
	private Socket sock;
	private PrintWriter out;
	private int userId = 0;
	private int userIdTo = 0;
	private static HashSet<Server> serverPool = new HashSet<>();
	private Session session;

	public static Db db;

	private Server(Socket sock) throws IOException {
		this.sock = sock;
	}

	public static void main(String[] args) throws IOException {
		ServerSocket ssock = null;
		try {
			Properties props = new Properties();
			props.load(new FileInputStream("Server.properties"));
			Server.db = new Db(props.getProperty("dbDriver"), props.getProperty("dbUrl"));
			port = Integer.parseInt(props.getProperty("port"));
			ssock = new ServerSocket(port);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "While binding port " + port + "\n" + e);
			System.exit(1);
		}

		JFrame mainWindow = new JFrame("Communicator server on port " + port);
		mainWindow.setSize(800, 500);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container interior = mainWindow.getContentPane();
		interior.setLayout(new BorderLayout());
		Container header = new Panel();
		interior.add(header, BorderLayout.SOUTH);
		header.setLayout(new GridLayout(1, 6));
		header.add(new JLabel("Registered users:", JLabel.RIGHT));
		nRegisteredUsers = new JLabel("0", JLabel.LEFT);
		header.add(nRegisteredUsers);
		header.add(new JLabel("Saved messages:", JLabel.RIGHT));
		nMessages = new JLabel("0", JLabel.LEFT);
		header.add(nMessages);
		header.add(new JLabel("Active threads:", JLabel.RIGHT));
		nThreadsLabel = new JLabel("0", JLabel.LEFT);
		header.add(nThreadsLabel);
		logWindow = new JTextArea();
		logWindow.setEditable(false);
		scrollPane = new JScrollPane(logWindow);
		interior.add(logWindow, BorderLayout.CENTER);
		Dimension dim = mainWindow.getToolkit().getScreenSize();
		Rectangle abounds = mainWindow.getBounds();
		mainWindow.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
		mainWindow.setVisible(true);
		refreshView();
		log("Server started");

		for (;;) {
			Socket sock = ssock.accept();
			new Thread(new Server(sock)).start();
		}
	}

	private synchronized static void refreshView() {
		nThreadsLabel.setText("" + serverPool.size());
		nRegisteredUsers.setText("" + User.getUsersCount());
		nMessages.setText("" + Message.getMessagesCount());
	}

	public static void log(String message) {
		logWindow.append(message + "\n");
		scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
	}

	@Override
	public void run() {
		log("Client " + sock.getRemoteSocketAddress() + " connected");
		serverPool.add(this);
		refreshView();
		try {
			out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			String suggestedSession = in.readLine();
			log("Suggested session got from the client " + sock.getRemoteSocketAddress() + " " + suggestedSession);
			session = new Session(suggestedSession, 0);
			log("Offered session sent to the client " + sock.getRemoteSocketAddress() + " " + session.toString());
			out.println(session.toString());
			userId = session.getUserId();
			userIdTo = session.getUserIdTo();
			log("Session " + session.toString() + " data { "
					+ (userId <= 0 ? "not-logged-in" : new User(userId).toString()) + " \u2192 "
					+ (userIdTo <= 0 ? "none" : new User(userIdTo).toString()) + " }");
			mainLoop: for (;;) {
				String s = null;
				try {
					s = in.readLine();
				} catch (SocketException e) {
					break;
				}
				if (s == null)
					break;
				/*
				 * interpretation of a command/data sent from clients
				 */
				if (s.charAt(0) == '/') {
					// out.println("You entered a command " + s);
					StringTokenizer st = new StringTokenizer(s);
					String cmd = st.nextToken();
					switch (cmd) {
					case "/register": // register email password nick - register new user
						try {
							String email = st.nextToken(), password = st.nextToken(), nick = st.nextToken();
							User u = new User(email, password, nick);
							if (u.getId() > 0) {
								out.println("The user was successfully registered with id " + u.getId());
								refreshView();
							} else {
								out.println("The user cannot be registered");
							}
						} catch (NoSuchElementException ex) {
							out.println("Not enough parameters");
						}
						break;
					case "/list": // list - show all registered users
						int[] ids = User.getUsersIds();
						for (int id : ids) {
							User user = new User(id);
							out.println(user.toString());
						}
						break;
					case "/login": // login email password - log into the system, without parameters - log out
						if (st.hasMoreTokens()) {
							String login = st.nextToken();
							if (st.hasMoreTokens()) {
								String password = st.nextToken();
								User user = new User(login, password);
								if (user.getId() > 0) {
									userId = user.getId();
									out.println("Welcome on the board, " + user.toString());
									session.update(userId);
								} else {
									out.println("Invalid password");
								}
							} else {
								out.println("No password provided");
							}
						} else {
							if (userId > 0) {
								out.println("Bye bye " + new User(userId).toString());
								userId = 0;
								session.update(userId);
							} else {
								out.println("Log in to log out");
							}
						}
						break;
					case "/to": // to nick - set recipient for further messages, without parameters - clear the
								// setting
						if (st.hasMoreTokens()) {
							String receiver = st.nextToken();
							List<Integer> users = new ArrayList<Integer>(); //list of message receiver 
							
							//retrieve group id if exists; otherwise -1
							int groupId = Group.groupExists(receiver);
							
							if(groupId != -1) {
								Group group = new Group(groupId, receiver);
								users = group.getGroupMembers(groupId);
							}else {
								users.add(new User(receiver).getId());
							}
							
							for (int id : users) {
								if (id != userId) {
									
									User user = new User(id);
									if (user.getId() > 0) {
										userIdTo = user.getId();
										out.println("Messages will be sent to " + user.toString());
										session.update(userId, userIdTo);
									} else {
										out.println("User with " + receiver + " does not exist");
									}								
								}
							}
							
						} else {
							userIdTo = 0;
							out.println("Messages will be sent to none");
							session.update(userId, 0);
						}
						break;
					case "/who": // show info about logged users
						int n = 0;
						for (Server server : serverPool) {
							if (server == this)
								out.print("(me) ");
							out.print(server.userId > 0 ? new User(server.userId).toString() : "not-logged-in");
							if (server == this && userIdTo > 0) {
								out.println(" to " + new User(userIdTo).toString());
							} else {
								out.println();
							}
							if (server.userId > 0)
								n++;
						}
						out.println("Clients: " + serverPool.size() + ", logged-in: " + n);
						break;
					case "/get": // show all our messages from the database
						if (userId > 0) {
							int[] mIds = Message.getIds(userId);
							for (int id : mIds) {
								Message m = new Message(id);
								out.println(m.toString());
							}
						} else {
							out.println("Log in to get messages");
						}
						break;
					case "/group": // show all our messages from the database
						if (userId > 0) {
							try {
								String groupName = st.nextToken();
								int groupId = Group.groupExists(groupName);
								if (groupId == -1) {
									groupId = Group.createGroup(groupName);
								}

								if (!User.isMember(userId, groupId)) {
									User.addToGroup(userId, groupId);
									out.println("You are in group " + groupName);
								} else {
									out.println("You are arleady in the group " + groupName);
								}

							} catch (NoSuchElementException ex) {
								out.println("Not enough parameters");
							}
						} else {
							out.println("Log in to get into group");
						}
						break;

					case "/showMembers": 
						if (userId > 0) {
							try {
								String groupName = st.nextToken();
								int groupId = Group.groupExists(groupName);
								if (groupId == -1) {
									out.println("Group " + groupName + " does not exist");
									break;
								}
								
								Group group = new Group (groupId, groupName);
								List<Integer> users = new ArrayList<Integer>();
								
								if (User.isMember(userId, groupId)) {
									users = group.getGroupMembers(groupId);
									out.println("You are in group " + groupName);
								}else {
									out.println("You are not a member of the group "+ groupName);
								}

								if (!users.isEmpty()) {
									for (int id : users) {
										if (id != userId) {

											User user = new User(id);
											out.println(user.getNick());
										}

									}
								}

							} catch (NoSuchElementException ex) {
								out.println("Not enough parameters");
							}
						} else {
							out.println("Log in to get group members");
						}
						break;

					case "/leaveGroup": // show all our messages from the database
						if (userId > 0) {
							try {
								String groupName = st.nextToken();
								int groupId = Group.groupExists(groupName);
								if (groupId == -1) {
									out.println("Group " + groupName + " does not exist");
									break;
								}

								Group group = new Group(groupId, groupName);
								List<Integer> users = new ArrayList<Integer>();
								
								if (User.isMember(userId, groupId)) {
									users = group.getGroupMembers(groupId);
									out.println("You are in group " + groupName);
								}else {
									out.println("You are not a member of the group "+ groupName);
								}

								if (!users.isEmpty()) {

									User user = new User(userId);
									out.println(user.leaveGroup(groupId));

									if (users.size() == 1) {
										group.closeGroup();
									}

								}

							} catch (NoSuchElementException ex) {
								out.println("Not enough parameters");
							}
						} else {
							out.println("Log in to get group members");
						}
						break;
					case "/exit": // close the client
						sock.close();
						break mainLoop;
					default:
						out.println("Unknown command " + cmd);
					}
				} else { // redistribution of message
					if (userId > 0) {
						int n = 0;
						for (Server server : serverPool) {
							if (server != this && server.userId > 0 && server.userId == userIdTo) {
								Message m = new Message(userId, server.userId, s);
								out.println(m); // to me
								server.out.println(m); // to recipient
								n++;
							}
						}
						out.println("Message sent to " + n + " client(s)");
						refreshView();
					} else {
						out.println("You have to log in first");
					}
				}
			}
		} catch (IOException ex) {
			log("Exception: " + ex.getMessage());
		}
		log("Client " + sock.getRemoteSocketAddress() + " disconnected");
		serverPool.remove(this);
		refreshView();
	}
}
