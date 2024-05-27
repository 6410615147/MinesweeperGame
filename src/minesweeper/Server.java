package minesweeper;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static ServerSocket server;
    private static List<PlayerThread> players = new ArrayList<>();
    private static final int BOARD_SIZE = 5;
    private static final int MINES = 5;
    private static int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private static boolean[][] mines = new boolean[BOARD_SIZE][BOARD_SIZE];
    private static int playerTurn = 0;
 
    public Server(){
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");

            createBoard();
 
            try {
                while (players.size() < 2) {
                    Socket player = server.accept();
                    PlayerThread playerThread = new PlayerThread(player, players.size());

                    players.add(playerThread);
                    System.out.println("Player " + players.size() + " accepted");

                    playerThread.start();
                }
            } finally {
                System.out.println("Closing connection");
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
                    if (input != null && playerIndex == playerTurn) {
                        String[] parts = input.split(",");
                        if (input.startsWith("Win")) {
                            String playerWin = parts[1];
                            handleWin(playerWin);
                        } else {
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
                    player.out.println("Game Over," + x + "," + y);
                }
            } else {
                int mineCount = board[x][y];
                for (PlayerThread player : players) {
                    player.out.println(mineCount + "," + x + "," + y);
                    player.out.println("Player " + playerTurn + " Turn");
                }
            }
        }

        private void handlePinged(int x, int y) {
            for (PlayerThread player : players) {
                player.out.println("P," + x + "," + y);
                player.out.println("Player " + playerTurn + " Turn");
            }
        }
        
        private void handleWin(String playerWin) {
            for (PlayerThread player : players) {
                player.out.println("Win " + playerWin);
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
