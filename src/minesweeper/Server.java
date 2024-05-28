package minesweeper;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static ServerSocket server;
    private static PlayerThread[] players = {null, null};
    private static final int BOARD_SIZE = 5;
    private static final int MINES = 5;
    private static int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private static boolean[][] mines = new boolean[BOARD_SIZE][BOARD_SIZE];
    private static int playerTurn = 0;
    private static int remainMine = MINES;
 
    public Server(){
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");

            createBoard();
 
            try {
                while (true) {
                    Socket player = server.accept();
                    PlayerThread playerThread;

                    for (int i = 0; i < players.length; i++) {
                        if (players[i] == null) {
                            playerThread = new PlayerThread(player, i);
                            System.out.println("Player " + (i+1) + " accepted");
                            players[i] = playerThread;
                            playerThread.start();
                            break;
                        }
                    }
                }
            } finally {
                server.close();
            }
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
    }

    private static void createBoard(){
        Random rand = new Random();
        int placedMines = 0;
        while (placedMines < MINES) {
            int x = rand.nextInt(BOARD_SIZE);
            int y = rand.nextInt(BOARD_SIZE);
            if (!mines[x][y]) {
                mines[x][y] = true;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        if (x + i >= 0 && x + i < BOARD_SIZE && y + j >= 0 && y + j < BOARD_SIZE) {
                            board[x+i][y+j] += 1;
                        }
                    }
                }
                placedMines++;
            }
        }
    }

    public static void resetBoard() {
        board = new int[BOARD_SIZE][BOARD_SIZE];
        mines = new boolean[BOARD_SIZE][BOARD_SIZE];
        playerTurn = 0;
        remainMine = MINES;
        createBoard();
    }

    private static class PlayerThread extends Thread {
        private Socket player;
        private int playerIndex;
        private BufferedReader in;
        private PrintWriter out;

        public PlayerThread(Socket playerSocket, int playerIndex) throws IOException {
            this.player = playerSocket;
            this.playerIndex = playerIndex;
            in = new BufferedReader(new InputStreamReader(player.getInputStream()));
            out = new PrintWriter(player.getOutputStream(), true);
        }

        public void run() {
            try {
                out.println("playerIndex:" + playerIndex);
                
                while (true) {
                    String input = in.readLine();

                    if (input.equals("restart")) {
                        handleRestart();
                    } else if (input != null && playerIndex == playerTurn) {
                        if (input.equals("win")) {
                            handleWin();
                        } else {
                            String[] parts = input.split(",");
                            String action = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            switchTurn();
                            if (action.equals("click")) {
                                handleClicked(x, y);   
                            } else {
                                handlePinged(x, y);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                players[playerIndex] = null;
                System.out.println("Player " + (playerIndex + 1) + " disconnected");
                handleRestart();
                
                try {
                    player.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }

        private void handleClicked(int x, int y) {
            if (mines[x][y]) {
                for (PlayerThread player : players) {
                    if (player != null) {
                        player.out.println("Hit Mine," + x + "," + y);
                    }
                }
            } else {
                int mineAround = board[x][y];
                for (PlayerThread player : players) {
                    if (player != null) {
                        player.out.println(mineAround + "," + x + "," + y);
                        player.out.println("Player " + playerTurn + " Turn");
                    }
                }
            }
        }

        private void handlePinged(int x, int y) {
            if (!mines[x][y]) {
                for (PlayerThread player : players) {
                    if (player != null) {
                        player.out.println("Not Mine," + x + "," + y);
                    }
                }
            } else {
                remainMine--;
                for (PlayerThread player : players) {
                    if (player != null) {
                        player.out.println("P," + x + "," + y + "," + remainMine);
                        player.out.println("Player " + playerTurn + " Turn");
                    }
                }
            }
        }
        
        private void handleWin() {
            for (PlayerThread player : players) {
                if (player != null) {
                    player.out.println("Win");
                }
            }
        }
        private void handleRestart() {
            resetBoard();
            for (PlayerThread player : players) {
                if (player != null) {
                    player.out.println("Restart");
                }
            }
        }

        private void switchTurn() {
            playerTurn = (playerTurn + 1) % 2;
        }
    }
    
    public static void main(String args[]) {
        Server server = new Server();
    }
}
