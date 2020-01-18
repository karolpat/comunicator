/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * @author Mariusz
 */
public class Client extends JFrame implements ActionListener, KeyListener, WindowListener, Runnable {

    private final JTextField textField;
    private final JScrollPane scrollPane;
    private final JTextArea mainPanel;
    private final JButton bok;
    private PrintWriter out = null;
    private BufferedReader in = null;

    private InetAddress addr;
    private int port;
    private boolean reconnect;
    private String session;

    public Client(String title) {
        super(title);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container interior = getContentPane();
        interior.setLayout(new BorderLayout());
        mainPanel = new JTextArea();
        mainPanel.setEditable(false);
        scrollPane = new JScrollPane(mainPanel);
        interior.add(scrollPane, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        textField = new JTextField();
        bottom.add(textField, BorderLayout.CENTER);
        bok = new JButton("OK");
        bok.addActionListener(this);
        textField.addKeyListener(this);
        bottom.add(bok, BorderLayout.EAST);
        interior.add(bottom, BorderLayout.SOUTH);
        addWindowListener(this);
        Dimension dim = getToolkit().getScreenSize();
        Rectangle abounds = getBounds();
        setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
        session = null;
    }

    public void connect() throws IOException {
        // defaults
        addr = InetAddress.getByName("localhost");
        port = 8888;
        reconnect = false;

        // properties overloading
        Properties props = new Properties();
        props.load(new FileInputStream("Client.properties"));
        addr = InetAddress.getByName(props.getProperty("host"));
        port = Integer.parseInt(props.getProperty("port"));
        reconnect = Boolean.parseBoolean(props.getProperty("reconnect"));
        String connectTo = addr.getHostAddress() + ":" + port;
        Socket sock = new Socket(addr.getHostName(), port);
        out = new PrintWriter(sock.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        if(session != null) {
            out.println(session);
        } else {
            out.println(""); // request for new  session
        }
        String offeredSession = in.readLine();
        if(session == null || !offeredSession.equals(session)) {
            session = offeredSession;
        }
        setTitle("Connected to " + connectTo + " [" + session + "]");
    }

    public static void main(String[] args) {
        Client f = new Client("Communicator client");

        try {
            f.connect();
            File autoStart = new File("Client.autostart");
            if(autoStart.exists()) {
                f.sendBatch(autoStart);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            System.exit(1);
        }

        new Thread(f).start();
        f.setVisible(true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            bok.doClick();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
        textField.requestFocus();
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String s = textField.getText();
        if (s.equals("")) return;
        try {
            if(out != null) {
                append("SEND " + s);
                out.println(s);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
            System.exit(0);
        }
        textField.setText(null);
    }

    public void sendBatch(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                append("SEND " + s);
                out.println(s);
            }
        } catch(IOException ex) {
            out.println("Error while sending batch to server");
        }
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                String s = (in != null) ? in.readLine() : null;
                if (s == null) {
                    if(reconnect) {
                        try {
                            in = null;
                            out = null;
                            setTitle("Trying to reconnect...");
                            Thread.sleep(1000);
                            connect();
                        } catch(Exception e) {}
                    } else {
                        JOptionPane.showMessageDialog(null, "Connection closed by the server");
                        System.exit(0);
                    }
                }
                if(s != null) {
                    append("RECV " + s);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e);
                System.exit(0);
            }
        }
    }

    private void append(String s) {
        mainPanel.append(s + "\n");
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
    }
}