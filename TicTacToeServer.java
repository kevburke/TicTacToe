// Fig. 18.8: TicTacToeServer.java
// This class maintains a game of Tic-Tac-Toe for two client applets.'''''''''''''''''''''

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicTacToeServer extends JFrame {
   private char[] board;                           //board array
   private JTextArea outputArea;                   //area to output info
   private Player[] players;                       //player array of 2
   private ServerSocket server;
   private int currentPlayer;                      //variable for switching players
   private final int PLAYER_X = 0, PLAYER_O = 1;   //2 players
   private final char X_MARK = 'X', O_MARK = 'O';  //marks on board
   private char WinResult = ' ';                   //flag to declare winner
   private int count=0;                            //counter to test number of moves
   private boolean done = false;                   //
   // set up tic-tac-toe server and GUI that displays messages

   ExecutorService TicTac = Executors.newFixedThreadPool(2);
   private static final Logger logger = Logger.getLogger("logs");
   public TicTacToeServer()
   {
      super( "Tic-Tac-Toe Server" );

      board = new char[ 9 ];
      players = new Player[ 2 ];
      currentPlayer = PLAYER_X;

      // set up ServerSocket
      try {
         server = new ServerSocket( 12345, 2 );    //port number of connections allowed
      }

      // process problems creating ServerSocket
      catch( IOException ioException ) {
         ioException.printStackTrace();
         logger.log(Level.INFO, "server Socket error");
         System.exit( 1 );
      }
      // set up JTextArea to display messages during execution
      outputArea = new JTextArea();
      getContentPane().add( outputArea, BorderLayout.CENTER );    //Jtext area on server side
      outputArea.setText( "Server awaiting connections\n" );

      setSize( 300, 300 );
      setVisible( true );

   } // end TicTacToeServer constructor

   // wait for two connections so game can be played
   public void execute()
   {
      TicTac.execute(new Runnable() {              //executor framework execute
                        @Override
                        public void run() {
                           // wait for each client to connect
                           for ( int i = 0; i < players.length; i++ ) {

                              // wait for connection, create Player, start thread
                              try {
                                 players[ i ] = new Player( server.accept(), i );//on first connection 1 thread started
                                 players[ i ].start();      //call start on client X
                              }

                              // process problems receiving connection from client
                              catch( IOException ioException ) {
                                 ioException.printStackTrace();
                                 System.exit( 1 );
                              }
                           }
                           // Player X is suspended until Player O connects.
                           // Resume player X now.
                           synchronized ( players[ PLAYER_X ] ) {
                              players[ PLAYER_X ].setSuspended( false );
                              players[ PLAYER_X ].notify();
                           }
                        }
                     }
      );

   }  // end method execute

   // utility method called from other threads to manipulate 
   // outputArea in the event-dispatch thread
   private void displayMessage( final String messageToDisplay )
   {
      // display message from event-dispatch thread of execution
      SwingUtilities.invokeLater(
              new Runnable() {  // inner class to ensure GUI updates properly

                 public void run() // updates outputArea
                 {
                    outputArea.append( messageToDisplay );
                    outputArea.setCaretPosition(
                            outputArea.getText().length() );
                 }
              }  // end inner class
      ); // end call to SwingUtilities.invokeLater
   }

   // Determine if a move is valid. This method is synchronized because 
   // only one move can be made at a time.
   public synchronized boolean validateAndMove( int location, int player )
   {
      // while not current player, must wait for turn
      while ( player != currentPlayer ) {

         // wait for turn
         try {
            wait();
         }

         // catch wait interruptions
         catch( InterruptedException interruptedException ) {
            interruptedException.printStackTrace();
         }
      }
      // if location not occupied, make move
      if ( !isOccupied( location ) ) {
         count++;                      //increment counter on valid move
         // set move in board array
         board[ location ] = currentPlayer == PLAYER_X ? X_MARK : O_MARK;

         // change current player
         currentPlayer = ( currentPlayer + 1 ) % 2;

         // let new current player know that move occurred
         players[ currentPlayer ].otherPlayerMoved( location );

         notify(); // tell waiting player to continue

         // tell player that made move that the move was valid
         return true;
      }

      // tell player that made move that the move was not valid
      else
         return false;

   } // end method validateAndMove

   // determine whether location is occupied
   public boolean isOccupied( int location )
   {
      if ( board[ location ] == X_MARK || board [ location ] == O_MARK )
         return true;
      else
         return false;
   }

   // place code in this method to determine whether game over 
   public synchronized boolean isGameOver()
   {
      if (WinResult == ' ') {
         for (int i = 0; i < board.length; i += 3) {                                //need +3 to move to next row
            if (board[0 + i] == 'X' && board[1 + i] == 'X' && board[2 + i] == 'X') {//check combination
               WinResult = 'X';                                                     //winner is X
               return true;
            } else if (board[0 + i] == 'O' && board[1 + i] == 'O' && board[2 + i] == 'O') {//check combination
               WinResult = 'O';                                                     //winner is O
               return true;
            }
         }
         for (int x = 0; x < 3; x++) {
            if (board[0 + x] == 'X' && board[3 + x] == 'X' && board[6 + x] == 'X') {//check combination
               WinResult = 'X';                                                     //winner is X
               return true;
            } else if (board[0 + x] == 'O' && board[3 + x] == 'O' && board[6 + x] == 'O') {//check combination
               WinResult = 'O';                                                      //winner is O
               return true;
            }
         }
         if (board[0 ] == 'X' && board[4] == 'X' && board[8] == 'X') {              //check combination
            WinResult = 'X';                                                        //winner is X
            return true;
         }
         if (board[2] == 'X' && board[4] == 'X' && board[6] == 'X') {               //check combination
            WinResult = 'X';                                                        //winner is X
            return true;
         }
         if (board[0 ] == 'O' && board[4] == 'O' && board[8] == 'O') {
            WinResult = 'O';                                                        //winner is O
            return true;
         }
         if (board[2] == 'O' && board[4] == 'O' && board[6] == 'O') {               //check combination
            WinResult = 'O';                                                        //winner is O
            return true;
         }
         else if (count==9){                                                        //9 moves validated
            WinResult= 'A';                                                         //no Winner
            return true;
         }
      }
      else
         return true;

      return false;
   }

   public static void main( String args[] )
   {
      TicTacToeServer application = new TicTacToeServer();
      application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      application.execute();
   }

   // private inner class Player manages each Player as a thread
   private class Player extends Thread {
      private Socket connection;
      private DataInputStream input;
      private DataOutputStream output;
      private int playerNumber;
      private char mark;
      protected boolean suspended = true;

      // set up Player thread
      public Player( Socket socket, int number )
      {
         playerNumber = number;

         // specify player's mark
         mark = ( playerNumber == PLAYER_X ? X_MARK : O_MARK );

         connection = socket;

         // obtain streams from Socket
         try {
            input = new DataInputStream( connection.getInputStream() );
            output = new DataOutputStream( connection.getOutputStream() );
         }

         // process problems getting streams
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit(1);
         }
      } // end Player constructor

      // send message that other player moved
      public void otherPlayerMoved( int location )
      {
         // send message indicating move
         try {
            if(isGameOver())           //if game is over send other player message that player won
            {
               if(WinResult=='A'){    //
                  output.writeUTF("Draw Game");
                  count=0;
                  output.writeInt(location);
               }
               else {
                  output.writeUTF("Player" + " " + WinResult + " is Winner");
                  output.writeInt(location);
               }
            }
            else {
               output.writeUTF("Opponent moved");
               output.writeInt(location);
            }
         }
         // process problems sending message
         catch ( IOException ioException ) {
            ioException.printStackTrace();
         }
      }
      private void processMessage( String message )
      {
         // valid move occurred
         if (message.equalsIgnoreCase("newGame")) {
            //currentPlayer = ( currentPlayer + 1 ) % 2;
            WinResult= ' ';
            count = 0;
            for (int ii = 0; ii < board.length; ii++) {

               board[ii] =' ';
            }

            // let new current player know that move occurred
            try {
               players[currentPlayer].output.writeUTF("reSet");
            } catch (IOException e) {
               e.printStackTrace();
            }
            //isGameOver();
         }
      } // end method processMessage
      // control thread's execution
      public void run()
      {
         // send client message indicating its mark (X or O),
         // process messages from client
         try {
            displayMessage("Player " + (playerNumber ==
                    PLAYER_X ? X_MARK : O_MARK) + " connected\n");

            output.writeChar(mark); // send player's mark

            // send message indicating connection
            output.writeUTF("Player " + (playerNumber == PLAYER_X ?
                    "X connected\n" : "O connected, please wait\n"));

            // if player X, wait for another player to arrive
            if (mark == X_MARK) {
               output.writeUTF("Waiting for another player");

               // wait for player O
               try {
                  synchronized (this) {
                     while (suspended)
                        wait();
                  }
               }

               // process interruptions while waiting
               catch (InterruptedException exception) {
                  exception.printStackTrace();
               }

               // send message that other player connected and
               // player X can make a move
               output.writeUTF("Other player connected. Your move.");
            }

            // while game not over
            //String message;
            while (true) {
               while (!isGameOver()) {

                  // get move location from client
                  int location = input.readInt();

                  // check for valid move

                  if (validateAndMove(location, playerNumber)) {
                     displayMessage("\nlocation: " + location);

                     output.writeUTF("Valid move.");
                  } else
                     output.writeUTF("Invalid move, try again");
               }
               if (isGameOver()) {
                  if (WinResult == 'X' || WinResult == 'O') {
                     output.writeUTF("You Win. Congrats");

                  } else if (WinResult == 'A') {

                     output.writeUTF("Draw Game");
                  }
               }
               done=false;
               while (done==false) {

                  if (input.available() > 0) {
                     processMessage(input.readUTF());
                     done=true;
                  }
               }
            }
         }// end try

         // process problems communicating with client
         catch(IOException ioException){
            ioException.printStackTrace();
            System.exit(1);
         }

      } // end method run

      // set whether or not thread is suspended

      public void setSuspended( boolean status )
      {
         suspended = status;
      }
   } // end class Player
} // end class TicTacToeServer

/**************************************************************************
 * (C) Copyright 1992-2003 by Deitel & Associates, Inc. and               *
 * Prentice Hall. All Rights Reserved.                                    *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
