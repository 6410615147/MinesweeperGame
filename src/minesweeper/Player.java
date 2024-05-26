package minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Player {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static final int BOARD_SIZE = 10;
    private static JFrame frame = new JFrame("Minesweeper");
    private static JButton[][] buttons = new JButton[BOARD_SIZE][BOARD_SIZE];
    private static boolean gameActive = true;
    private static int playerIndex;
    private static int playerTurn = 0;
 
    public Player(){
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected");
 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String playerIndexResponse = in.readLine();
            playerIndex = Integer.parseInt(playerIndexResponse.split(":")[1]);
        }
        catch (UnknownHostException u) {
            System.out.println(u);
        }
        catch (IOException i) {
            System.out.println(i);
        }
    }

    private static void createFrame() {
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.PLAIN, 10));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].addMouseListener(new ButtonListener(i, j));
                frame.add(buttons[i][j]);
            }
        }

        frame.setVisible(true);
    }

    private static void updateBoard(String response) {
        String text = response.split(",")[0];
        int x = Integer.parseInt(response.split(",")[1]);
        int y = Integer.parseInt(response.split(",")[2]);
        buttons[x][y].setText(text);
        buttons[x][y].setEnabled(false);
    }

    private static void updateTurn(String response) {
        playerTurn = Integer.parseInt(response.split(" ")[1]);
        String message = (playerIndex == playerTurn) ? "Your turn" : "Opponent's turn";
        frame.setTitle("Minesweeper - " + message);
    }

    private static void endGame(String response) {
        int x = Integer.parseInt(response.split(",")[1]);
        int y = Integer.parseInt(response.split(",")[2]);
        buttons[x][y].setBackground(Color.RED);
        JOptionPane.showMessageDialog(frame, "Game Over Player " + (playerTurn+1) + " hit the mine!");
        gameActive = false;
    }

    private static class ButtonListener extends MouseAdapter {
        private int x, y;

        public ButtonListener(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void mouseClicked(MouseEvent e) {
            if (gameActive && playerIndex == playerTurn && buttons[x][y].isEnabled()) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    out.println("click," + x + "," + y);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    out.println("ping," + x + "," + y);
                }
            }
        }
    }
 
    public static void main(String args[]) throws IOException {
        Player player = new Player();
        createFrame();

        while (true) {
            String response = in.readLine();
            System.out.println("Server: " + response);

            if (response.startsWith("Game Over")) {
                endGame(response);
            } else if (response.startsWith("Player")) {
                updateTurn(response);
            } else {
                updateBoard(response);
            }
        }
    }
}
