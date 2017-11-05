package lab2_Final;

import java.util.ArrayList;
import java.util.Scanner;

class Othello {
	
	static char[][] board = new char[10][10];
	static int[] moves = new int[64];
	static boolean player = true;
	static int Depth = 12;
	static int ub, lb; 
	static int max = Integer.MIN_VALUE;
	static int bestMove;
	static boolean passp1,passp2;
	static int count = 0;
	
	static int weightMatrix[] = {0,   0,   0,   0,   0,   0,   0,   0,   0,   0, 
									0, 100, -20,  20,   5,   5,  20, -20, 100, 0,
									0, -20, -40,  -5,  -5,  -5,  -5, -40, -20, 0,
									0,  20,  -5,  15,   3,   3,  15,  -5,  20, 0, 
									0,   5,  -5,   3,   3,   3,   3,  -5,   5, 0,   
									0,   5,  -5,   3,   3,   3,   3,  -5,   5,   0,
									0,  20,  -5,  15,   3,   3,  15,  -5,  20,   0,
									0, -20, -40,  -5,  -5,  -5,  -5, -40, -20,   0,
									0, 100, -20,  20,   5,   5,  20, -20, 100,   0,
									0,   0,   0,   0,   0,   0,   0,   0,   0,   0,};

// up = -10, upRight = -9, Right = 1, downRight = 11, down = 10, downLeft = 9, left = -1, upLeft = -11
	static int[] directions = {-10, -9, 1, 11, 10, 9, -1, -11};
	
	public static void main(String args[]){
		initializeBoard();
		initializeMoves();
		startingInfo();
		startingInput();
	}
	
	private static void startingInfo() {
		System.out.println("\n\nYour player is W(White) and computer is B(Black)\n\n----------------Initial Board State-----------------\n");
		printBoard(board);
	}
	
	@SuppressWarnings("resource")
	private static void startingInput() {
		System.out.println("\n\n1) For SELF PLAY type 'play' and enter");
		System.out.println("2) For USER move type in  : "
				+ "row column. For example : 5 6");
		System.out.println("3) To exit enter 'exit'\n");
		
		

		System.out.print("Input : ");
		Scanner user_input = new Scanner(System.in);
		String move = user_input.nextLine();

		String[] position = move.split(" ");
		if(move.equals("play"))
				letsPlay(player);
		else if(move.equals("exit"))
			return;
		else if(position.length == 2) {
			try {
				int row = Integer.parseInt(position[0]);
				int column = Integer.parseInt(position[1]);
				letsPlayWithUser(player, (row*10 + column%10));
			}
			catch(Exception ex){
				System.out.println("Invalid input");
				startingInput();
			}
		} else {
			System.out.println("Invalid input");
			startingInput();
		}
		user_input.close();
	}

	private static void letsPlayWithUser(boolean player, int move) {
		int[] legalMoves = getLegalMoves(board, player);
		boolean validMove = false;
		
		for(int i=0; i<legalMoves.length; i++) {
			if(legalMoves[i] == move)  {
				move = legalMoves[i]; 
				validMove = true;
				break;
			}
		}
		
		if(validMove) {
			printLegalMoves(legalMoves,player);
			board = makeMove(move, board, player);
	
			System.out.println("Player " + getPlayer(player) + " Move : ("+(move/10)+","+(move%10)+")");
			printBoard(board);

			letsPlayWithComputer(!player);
		} else {
			System.out.println("Not possible move");
			startingInput();
		}
	}
	
	private static void letsPlayWithComputer(boolean player) {
		int[] legalMoves = getLegalMoves(board, player);
		
		if(legalMoves.length > 0) {
			printLegalMoves(legalMoves,player);
			int bestMove = agent(board, player, Integer.MIN_VALUE, Integer.MAX_VALUE, Depth);
			board = makeMove(bestMove, board, player);
	
			System.out.println("Player " + getPlayer(player) + " Move : ("+(bestMove/10)+","+(bestMove%10)+")");
			printBoard(board);
			
			max =  Integer.MIN_VALUE; ub = 0; lb =0;
		} else {
			System.out.println("No possible move");
		}
		legalMoves = getLegalMoves(board, !player);
		printLegalMoves(legalMoves, !player);
		startingInput();
	}
	
	private static void letsPlay(boolean player) {
		int[] legalMoves = getLegalMoves(board, player);
		
		if(legalMoves.length > 0) {
			printLegalMoves(legalMoves,player);
			int bestMove = agent(board, player, Integer.MIN_VALUE, Integer.MAX_VALUE, Depth);
			board = makeMove(bestMove, board, player);
	
			System.out.println("Best Move : ("+(bestMove/10)+","+(bestMove%10)+")");
			printBoard(board);
			
			max =  Integer.MIN_VALUE; ub = 0; lb =0;
			letsPlay(!player);
		} else {
			pass(player);
			if(passp1 && passp2)
				Calculate();
			else
				letsPlay(!player);		
		}
	}
	
	private static void pass(boolean player) {
		if(player)
			passp1 = true;
		else
			passp2 = true;		
	}

	private static void Calculate() {
		int w = 0, b = 0;
		for(int i=1; i<board.length; i++) {
			for(int j=1; j<board.length; j++) {
				if(board[i][j] == 'W')
					w++;
				if(board[i][j] == 'B')
					b++;
			}
		}
		
		System.out.println(" White player points : " + w);
		System.out.println(" Black player points : " + b);
	}

	private static int agent(char[][] board, boolean player, int minValue, int maxValue, int depth) {
		if(depth < 1){
			return evaluateFunction(board, player);
		}
		
		int l[] = getLegalMoves(board, player);

		for(int i=0; i<l.length; i++){            
			if(Depth == depth)
				board = copy();
			
			char[][] board1 = makeMove(l[i], board, player);
			int score = getScore(player, board1);
			
			if(score >= minValue && player) {
				minValue=score;
				ub = minValue;
			}		

			if(score < maxValue && !player){
				maxValue=score;
				lb = maxValue;
			}
		    
		    int val = -agent(board1, !player, ub, lb, depth-1);
		    
		    if(Depth == depth){
		        val = (ub-lb) + (weightMatrix[l[i]]);
			    if(val >= max){
			        max = val;
			        bestMove = l[i];
			    }
		    }
		}
		return bestMove;
	}
	
	private static int evaluateFunction(char[][] board, boolean player) {
		return (getScore(player, board)) + (totalWeight(player,board));
	}
	
	private static char[][] makeMove(int move, char[][] board, boolean player){
		board[move/10][move%10] = getPlayer(player);
        for(int i=0; i < directions.length; i++)
            board = makeFlips(move, directions[i], board, player);
        return board;
	}

	private static char[][] makeFlips(int move, int direction, char[][] board, boolean player) {
		int q = 0;
		int pos = move + direction;
		if(isValid(pos)){
			if(board[pos/10][pos%10] == getPlayer(player))
				q = 0;
			while(board[pos/10][pos%10] == getPlayer(!player)) {
				pos = pos + direction;
			}
			if(board[pos/10][pos%10] == getPlayer(player))
				q = pos;		
		}

	    if(q == 0) 
	    	return board;
	    pos = move + direction;
	    while(pos != q) {
	        board[(pos/10)][(pos%10)] = getPlayer(player);
	        pos = pos + direction;
	    }
	    return board;
	}
	
	private static char[][] copy() {
		char[][] cboard = new char[10][10];
		for(int i=1; i < cboard.length; i++) {
			for(int j=1; j < cboard.length; j++) {
				cboard[i][j] = board[i][j];
			}
		}
		return cboard;
	}

	private static int getScore(boolean player, char[][] board) {
		int player1 = 0;
		int player2 = 0;
		for(int i = 11; i <= 88; i++){
			if(i%10 >=1 && i%10 <= 8) {
				if(board[i/10][i%10] == getPlayer(player)) 
					player1++;
				if(board[i/10][i%10] == getPlayer(!player)) 
					player2++; 
			}
		}
		return player1 - player2;
	}

	private static int totalWeight(boolean player,char[][] board){ 
	   	int total=0;  	
	   	for(int i=0;i<moves.length;i++){
	   		if(board[moves[i]/10][moves[i]%10] == getPlayer(player)) 
	   			total = total + weightMatrix[moves[i]];
	   		if(board[moves[i]/10][moves[i]%10] == getPlayer(!player)) 
	   			total = total - weightMatrix[moves[i]];
	   	}
	   	return total;
	}

	private static void printLegalMoves(int[] p, boolean player) {
		System.out.print("Legal Moves for player " + getPlayer(player) + " : ");
		for(int i=0; i<p.length;i++)
			System.out.print("(" + p[i]/10 + "," + p[i]%10 + ")" + " ");
		System.out.println();
	}
    
	private static int[] getLegalMoves(char[][] board, boolean player) {
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for(int i=0; i<moves.length; i++) {
			if(validMove(moves[i], board, player))
				arr.add(moves[i]);
		}

		int[] l = new int[arr.size()];
		for(int j=0; j<arr.size(); j++)
			l[j] = arr.get(j);
		return l;
	}

	private static boolean validMove(int move, char[][] board, boolean player) {
		boolean flag = false;
		if(board[move/10][move%10] == getPlayer(player) || board[move/10][move%10] == getPlayer(!player))
			return false;
		
		for(int k=0; k<directions.length; k++) {
			boolean b = validBox(move, directions[k], player, board);
			if(b) flag = true;	
		}
		return flag;
	}
	
	private static boolean validBox(int move, int direction, boolean player, char[][] board) {
		int pos = move + direction;
		if(isValid(pos)){
			if(board[pos/10][pos%10] == getPlayer(player))
				return false;
			while(board[pos/10][pos%10] == getPlayer(!player)) {
				pos = pos + direction;
			}
			if(isValid(pos) && board[pos/10][pos%10] == getPlayer(player))
				return true;
		}
		return false;
	}

	private static boolean isValid(int pos) {
		for(int i=0; i<moves.length; i++)
			if(moves[i] == pos)
				return true;
		return false;
	}

	private static char getPlayer(boolean player) {
		if(player)
			return 'W';
		return 'B';
	}

	private static void initializeBoard() {
		for(int j=1; j<board.length; j++) {
			board[0][j] = (char)j;
		}
		
		for(int i=1; i<board.length; i++) {
			board[i][0] = (char)i; 
		}
		
		for(int i=1; i<board.length; i++) {
			for(int j=1; j<board.length; j++) {
				board[i][j] = ' '; 
			}
		}
		board[4][4] = 'B';
		board[5][5] = 'B';
		board[4][5] = 'W';
		board[5][4] = 'W';
	}
	
	private static void initializeMoves(){
		int count = 0;
		for(int i=11; i<=88; i++){
			if(i%10 >=1 && i%10 <= 8)
				moves[count++] = i;
		}
	}

	private static void printBoard(char[][] board){
    	System.out.print("   ");
    	
    	for(int i=1; i < board.length-1 ; i++)
    		System.out.print("| " + i + " ");
    	System.out.println("|");
    	
    	printHorizontalBorder();
    	
    	for(int i=1; i < board.length-1 ; i++){
    		System.out.print(" " + i + " ");
    		for(int j=1 ; j < board.length-1 ; j++){
    			if(board[i][j] == 'W'){
    				System.out.print("| W ");
    			}else if(board[i][j] == 'B'){
    				System.out.print("| B ");
    			}else{
    				System.out.print("|   ");
    			}
    		}
    		
    		System.out.println("| "+ i + " ");
    		printHorizontalBorder();
    	}
    	System.out.print("   ");
    	
    	for(int i=1 ; i < board.length-1 ; i++)
    	    System.out.print("| " + i + " ");
    	System.out.println("|\n");
    }

    private static void printHorizontalBorder() {
    	
    	System.out.print("---");
    	
    	for(int i = 1 ; i < board.length-1 ; i++){
    		System.out.print("|---");
    	}
    	
    	System.out.println("|---");
    }
}

