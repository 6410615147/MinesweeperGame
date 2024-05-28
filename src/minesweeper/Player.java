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
    private static final int BOARD_SIZE = 5;
    private static final int MINES = 5;
    private static JFrame frame = new JFrame("Minesweeper");
    private static JButton[][] buttons = new JButton[BOARD_SIZE][BOARD_SIZE];
    private static boolean gameActive = true;
    private static int playerIndex;
    private static int playerTurn = 0;
    private static JTextField textBox = new JTextField();
    private static int remainMine = MINES;
 
    public Player(){
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected");
 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            playerIndex = Integer.parseInt(in.readLine().split(":")[1]);
            frame.setTitle("Minesweeper (Player " + (playerIndex+1) + ")");
        }
        catch (UnknownHostException u) {
            System.out.println(u);
        }
        catch (IOException i) {
            System.out.println(i);
        }
    }

    private static void createFrame() {
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.PLAIN, 10));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].addMouseListener(new ButtonListener(i, j));
                boardPanel.add(buttons[i][j]);
            }
        }
        frame.add(boardPanel, BorderLayout.CENTER);

        JPanel controPanel = new JPanel(new BorderLayout());

        textBox.setText("Remaing Mines: " + remainMine);
        textBox.setEditable(false);
        textBox.setHorizontalAlignment(JTextField.CENTER);
        textBox.setFont(new Font("Arial", Font.PLAIN, 10));

        JButton restartButton = new JButton("Restart");
        restartButton.setFont(new Font("Arial", Font.PLAIN, 10));
        restartButton.setFocusPainted(false);
        restartButton.addActionListener(e -> {
            out.println("restart");
        });

        controPanel.add(textBox, BorderLayout.CENTER);
        controPanel.add(restartButton, BorderLayout.EAST);

        frame.add(controPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void updateBoard(String response) {
        String[] parts = response.split(",");
        String text = parts[0];
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        if (text.equals("P")) {
            remainMine = Integer.parseInt(parts[3]);
        }
        buttons[x][y].setText(text);
        buttons[x][y].setEnabled(false);
        checkWin();
    }

    private static void updateTurn(String response) {
        playerTurn = Integer.parseInt(response.split(" ")[1]);
        String message = (playerIndex == playerTurn) ? "Your turn" : "Opponent's turn";
        frame.setTitle("Minesweeper (Player " + (playerIndex+1) + ") - " + message);
    }

    private static void endGame(String response) {
        String[] parts = response.split(",");
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        buttons[x][y].setBackground(Color.RED);
        if (parts[0].startsWith("Hit")) {
            JOptionPane.showMessageDialog(frame, "Game Over! Player " + (playerTurn+1) + " hit the mine!");    
        } else {
            JOptionPane.showMessageDialog(frame, "Game Over! Player " + (playerTurn+1) + " ping wrong!");
        }
        gameActive = false;
    }

    private static void winGame(String response) {
        JOptionPane.showMessageDialog(frame, "Congratulation!");
        gameActive = false;
    }

    private static void checkWin() {
        boolean allDisabled = true;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (buttons[i][j].isEnabled()) {
                    allDisabled = false;
                }
            }
        }
        if (allDisabled) {
            out.println("win");
        }
    }

    private static void restart() {
        gameActive = true;
        playerTurn = 0;
        frame.setTitle("Minesweeper (Player " + (playerIndex+1) + ")");
        remainMine = MINES;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackground(null);
            }
        }
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

            if (response.startsWith("Hit") || response.startsWith("Not")) {
                endGame(response);
            } else if (response.startsWith("Player")) {
                updateTurn(response);
            } else if (response.equals("Win")) {
                winGame(response);
            } else if (response.equals("Restart")) {
                restart();
            } else {
                updateBoard(response);
            }

            textBox.setText("Remaining Mines: " + remainMine);
        }
    }
}
