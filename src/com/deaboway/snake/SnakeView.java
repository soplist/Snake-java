/**
 * <p>Title: Snake</p>
 * <p>Copyright: (C) 2007 The Android Open Source Project. Licensed under the Apache License, Version 2.0 (the "License")</p>
 * @author WangKang
 */

package com.deaboway.snake;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

/**
 * SnakeView: implementation of a simple game of Snake
 * 
 * 
 */
public class SnakeView extends TileView {

	private static final String TAG = "Deaboway";

	/**
	 * Current mode of application: READY to run, RUNNING, or you have already
	 * lost. static final ints are used instead of an enum for performance
	 * reasons.
	 */
	//game state,default value is ready
	private int mMode = READY;

	//4 state of the game:pause,ready,running,lose
	public static final int PAUSE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int LOSE = 3;

	//direction of the snake,default is north
	private int mDirection = NORTH;
	//next step direction,default is north
	private int mNextDirection = NORTH;

	//game direction:north,south,east,west
	private static final int NORTH = 1;
	private static final int SOUTH = 2;
	private static final int EAST = 3;
	private static final int WEST = 4;

	/**
	 * Labels for the drawables that will be loaded into the TileView class
	 */
	//3 kind of star
	private static final int RED_STAR = 1;
	private static final int YELLOW_STAR = 2;
	private static final int GREEN_STAR = 3;

	/**
	 * mScore: used to track the number of apples captured mMoveDelay: number of
	 * milliseconds between snake movements. This will decrease as apples are
	 * captured.
	 */
	//game score
	private long mScore = 0;

	//move delay
	private long mMoveDelay = 600;

	/**
	 * mLastMove: tracks the absolute time when the snake last moved, and is
	 * used to determine if a move should be made based on mMoveDelay.
	 */
	//last moment of last move
	private long mLastMove;

	/**
	 * mStatusText: text shows to the user in some run states
	 */
	//text view to show game state
	private TextView mStatusText;

	/**
	 * mSnakeTrail: a list of Coordinates that make up the snake's body
	 * mAppleList: the secret location of the juicy apples the snake craves.
	 */
	//snake ArrayList(element is coordinate)
	private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();

	//apple ArrayList(element is coordinate)
	private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();

	/**
	 * Everyone needs a little randomness in their life
	 */
	//random number
	private static final Random RNG = new Random();

	/**
	 * Create a simple handler that we can use to cause animation to happen. We
	 * set ourselves as a target and we can use the sleep() function to cause an
	 * update/invalidate to occur at a later date.
	 */
	// greate a Refresh Handler to build animation:use sleep()
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	//Handler
	class RefreshHandler extends Handler {

		//handle message queue
		@Override
		public void handleMessage(Message msg) {
			//update SnakeView object
			SnakeView.this.update();
			//forced redraw
			SnakeView.this.invalidate();
		}

		//delay send message
		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};

	/**
	 * Constructs a SnakeView based on inflation from XML
	 * 
	 * @param context
	 * @param attrs
	 */
	//constructor
	public SnakeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		//init SnakeView
		initSnakeView();
	}

	public SnakeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSnakeView();
	}

	//init
	private void initSnakeView() {
		//set focusable
		setFocusable(true);

		Resources r = this.getContext().getResources();

		//set tile image array
		resetTiles(4);

		//save 3 kind of image into array of Bitmap
		loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
		loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
		loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));

	}

	//start new game
	private void initNewGame() {
		//clear snake and apple
		mSnakeTrail.clear();
		mAppleList.clear();

		// For now we're just going to load up a short default eastbound snake
		// that's just turned north
		// greate new snake

		mSnakeTrail.add(new Coordinate(7, 7));
		mSnakeTrail.add(new Coordinate(6, 7));
		mSnakeTrail.add(new Coordinate(5, 7));
		mSnakeTrail.add(new Coordinate(4, 7));
		mSnakeTrail.add(new Coordinate(3, 7));
		mSnakeTrail.add(new Coordinate(2, 7));

		//next direction:north
		mNextDirection = NORTH;

		//2 apple at random position
		addRandomApple();
		addRandomApple();

		//move delay
		mMoveDelay = 600;
		//init score is 0
		mScore = 0;
	}

	/**
	 * Given a ArrayList of coordinates, we need to flatten them into an array
	 * of ints before we can stuff them into a map for flattening and storage.
	 * 
	 * @param cvec
	 *            : a ArrayList of Coordinate objects
	 * @return : a simple array containing the x/y values of the coordinates as
	 *         [x1,y1,x2,y2,x3,y3...]
	 */
	//transform coordinate ArrayList to integer array,put coordinate object's x and y into a int array
	private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
		int count = cvec.size();
		int[] rawArray = new int[count * 2];
		for (int index = 0; index < count; index++) {
			Coordinate c = cvec.get(index);
			rawArray[2 * index] = c.x;
			rawArray[2 * index + 1] = c.y;
		}
		return rawArray;
	}

	/**
	 * Save game state so that the user does not lose anything if the game
	 * process is killed while we are in the background.
	 * 
	 * @return a Bundle with this view's state
	 */
	//save state
	public Bundle saveState() {

		Bundle map = new Bundle();

		map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
		map.putInt("mDirection", Integer.valueOf(mDirection));
		map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
		map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
		map.putLong("mScore", Long.valueOf(mScore));
		map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));

		return map;
	}

	/**
	 * Given a flattened array of ordinate pairs, we reconstitute them into a
	 * ArrayList of Coordinate objects
	 * 
	 * @param rawArray
	 *            : [x1,y1,x2,y2,...]
	 * @return a ArrayList of Coordinates
	 */
	//transform integer array to coordinate ArrayList,put a int array has x and y into coordinate object
	private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
		ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

		int coordCount = rawArray.length;
		for (int index = 0; index < coordCount; index += 2) {
			Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
			coordArrayList.add(c);
		}
		return coordArrayList;
	}

	/**
	 * Restore game state if our process is being relaunched
	 * 
	 * @param icicle
	 *            a Bundle containing the game state
	 */
	//restore state
	public void restoreState(Bundle icicle) {

		setMode(PAUSE);

		mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
		mDirection = icicle.getInt("mDirection");
		mNextDirection = icicle.getInt("mNextDirection");
		mMoveDelay = icicle.getLong("mMoveDelay");
		mScore = icicle.getLong("mScore");
		mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
	}

	/*
	 * handles key events in the game. Update the direction our snake is
	 * traveling based on the DPAD. Ignore events that would cause the snake to
	 * immediately turn back on itself.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
	 */
	//listen use's keyboard operation,and handle it
	//handle the event of the key,make sure that snake can 90 degrees turn,can not 180 degrees turn
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {

		//up
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			//ready or lose state
			if (mMode == READY | mMode == LOSE) {
				/*
				 * At the beginning of the game, or the end of a previous one,
				 * we should start a new game.
				 */
				//init game
				initNewGame();
				//set mode running
				setMode(RUNNING);
				//update
				update();
				//return
				return (true);
			}

			//pause
			if (mMode == PAUSE) {
				/*
				 * If the game is merely paused, we should just continue where
				 * we left off.
				 */
				//set mode running
				setMode(RUNNING);
				update();
				//return
				return (true);
			}

			//if mode is running,if direction is not south,turn north
			if (mDirection != SOUTH) {
				mNextDirection = NORTH;
			}
			return (true);
		}

		//down
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			//if direction is not north,turn south
			if (mDirection != NORTH) {
				mNextDirection = SOUTH;
			}
			//return
			return (true);
		}

		//left
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			//if direction is not east,turn west
			if (mDirection != EAST) {
				mNextDirection = WEST;
			}
			//return
			return (true);
		}

		//right
		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			//if direction is not west,turn east
			if (mDirection != WEST) {
				mNextDirection = EAST;
			}
			//return
			return (true);
		}

		//other key,return super method onKeyDown
		return super.onKeyDown(keyCode, msg);
	}

	/**
	 * Sets the TextView that will be used to give information (such as "Game
	 * Over" to the user.
	 * 
	 * @param newView
	 */
	//set show state TextView
	public void setTextView(TextView newView) {
		mStatusText = newView;
	}

	/**
	 * Updates the current mode of the application (RUNNING or PAUSED or the
	 * like) as well as sets the visibility of textview for notification
	 * 
	 * @param newMode
	 */
	//set game mode
	public void setMode(int newMode) {

		//save current mode into oldMode
		int oldMode = mMode;
		//set new mode
		mMode = newMode;

		//if newMode is running and oldMode is not running,start game
		if (newMode == RUNNING & oldMode != RUNNING) {
			//set mStatusTextView invisible
			mStatusText.setVisibility(View.INVISIBLE);
			//update
			update();
			return;
		}

		Resources res = getContext().getResources();
		CharSequence str = "";

		//if newMode is pause
		if (newMode == PAUSE) {
			str = res.getText(R.string.mode_pause);
		}

		//if newMode is ready
		if (newMode == READY) {
			str = res.getText(R.string.mode_ready);
		}

		//if newMode is lose
		if (newMode == LOSE) {
			//show game score
			str = res.getString(R.string.mode_lose_prefix) + mScore
					+ res.getString(R.string.mode_lose_suffix);
		}

		//set text
		mStatusText.setText(str);
		//set view visible
		mStatusText.setVisibility(View.VISIBLE);
	}

	/**
	 * Selects a random location within the garden that is not currently covered
	 * by the snake. Currently _could_ go into an infinite loop if the snake
	 * currently fills the garden, but we'll leave discovery of this prize to a
	 * truly excellent snake-player.
	 * 
	 */
	//add random apple
	private void addRandomApple() {
		//new coordinate
		Coordinate newCoord = null;
		//prevent new apple appear at snake position
		boolean found = false;
		//find unit have suitable apple
		while (!found) {
			//random x
			int newX = 1 + RNG.nextInt(mXTileCount - 2);
			//random y
			int newY = 1 + RNG.nextInt(mYTileCount - 2);
			//new coordinate
			newCoord = new Coordinate(newX, newY);

			//Make sure it's not already under the snake
			//prevent new apple appear at snake position,assume not conflict
			boolean collision = false;

			int snakelength = mSnakeTrail.size();
			//compare with snake's coordinate
			for (int index = 0; index < snakelength; index++) {
				//if equal with one of snake's coordinate,conflict
				if (mSnakeTrail.get(index).equals(newCoord)) {
					collision = true;
				}
			}
			// if we're here and there's been no collision, then we have
			// a good location for an apple. Otherwise, we'll circle back
			// and try again
			//if have conflict,loop continue,if not conflict,end up loop,have new apple coordinate
			found = !collision;
		}

		if (newCoord == null) {
			Log.e(TAG, "Somehow ended up with a null newCoord!");
		}
		//new apple put in apple list(two apple have same coordinate is possible)
		mAppleList.add(newCoord);
	}

	/**
	 * Handles the basic update loop, checking to see if we are in the running
	 * state, determining if a move should be made, updating the snake's
	 * location.
	 */
	//update each kinds of action,include snake position,and update wall,apple,and so on
	public void update() {
		//if running state
		if (mMode == RUNNING) {

			long now = System.currentTimeMillis();

			//if delay time is over after last move time
			if (now - mLastMove > mMoveDelay) {
				//
				clearTiles();
				updateWalls();
				updateSnake();
				updateApples();
				mLastMove = now;
			}
			//redraw hadler thread sleep a unit of delay time
			mRedrawHandler.sleep(mMoveDelay);
		}

	}

	/**
	 * Draws some walls.
	 * 
	 */
	//update wall
	private void updateWalls() {
		for (int x = 0; x < mXTileCount; x++) {
			//top line
			setTile(GREEN_STAR, x, 0);
			//bottom line
			setTile(GREEN_STAR, x, mYTileCount - 1);
		}
		for (int y = 1; y < mYTileCount - 1; y++) {
			//left line
			setTile(GREEN_STAR, 0, y);
			//right line
			setTile(GREEN_STAR, mXTileCount - 1, y);
		}
	}

	/**
	 * Draws some apples.
	 * 
	 */
	//update apple
	private void updateApples() {
		for (Coordinate c : mAppleList) {
			setTile(YELLOW_STAR, c.x, c.y);
		}
	}

	/**
	 * Figure out which way the snake is going, see if he's run into anything
	 * (the walls, himself, or an apple). If he's not going to die, we then add
	 * to the front and subtract from the rear in order to simulate motion. If
	 * we want to grow him, we don't subtract from the rear.
	 * 
	 */
	//update snake
	private void updateSnake() {
		//grow sign
		boolean growSnake = false;

		//get snake head coordinate
		Coordinate head = mSnakeTrail.get(0);
		//init a new snake head coordinate
		Coordinate newHead = new Coordinate(1, 1);

		//current direction update to new direction
		mDirection = mNextDirection;

		//make new coordinate of snake head by direction
		switch (mDirection) {
		//if direction is east,x+1
		case EAST: {
			newHead = new Coordinate(head.x + 1, head.y);
			break;
		}
		//if direction is west,x-1
		case WEST: {
			newHead = new Coordinate(head.x - 1, head.y);
			break;
		}
		//if direction is north,y-1
		case NORTH: {
			newHead = new Coordinate(head.x, head.y - 1);
			break;
		}
		//if direction is south,y+1
		case SOUTH: {
			newHead = new Coordinate(head.x, head.y + 1);
			break;
		}
		}

		// Collision detection
		// For now we have a 1-square wall around the entire arena
		//conflict check,if snake head touch side line,end game
		if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
				|| (newHead.y > mYTileCount - 2)) {
			//set mode Lose
			setMode(LOSE);
			//return
			return;

		}

		// Look for collisions with itself
		//conflict check,if snake head touch snake body,end game
		int snakelength = mSnakeTrail.size();

		for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
			Coordinate c = mSnakeTrail.get(snakeindex);
			if (c.equals(newHead)) {
				//set mode lose
				setMode(LOSE);
				//return
				return;
			}
		}

		// Look for apples
		//check snake head touch apple
		int applecount = mAppleList.size();
		for (int appleindex = 0; appleindex < applecount; appleindex++) {
			Coordinate c = mAppleList.get(appleindex);
			if (c.equals(newHead)) {
				//if touched,remove apple
				mAppleList.remove(c);
				//add random apple
				addRandomApple();
				//add score
				mScore++;
				//update delay time
				mMoveDelay *= 0.9;
				//grow snake flag update
				growSnake = true;
			}
		}

		// push a new head onto the ArrayList and pull off the tail
		// add new coordinate at snake head
		mSnakeTrail.add(0, newHead);
		// except if we want the snake to grow
		// if snake not grow
		if (!growSnake) {
			//remove last coordinate
			mSnakeTrail.remove(mSnakeTrail.size() - 1);
		}

		int index = 0;
		//reset color,snake head is yellow,snake body is red
		for (Coordinate c : mSnakeTrail) {
			if (index == 0) {
				setTile(YELLOW_STAR, c.x, c.y);
			} else {
				setTile(RED_STAR, c.x, c.y);
			}
			index++;
		}

	}

	/**
	 * Simple class containing two integer values and a comparison function.
	 * There's probably something I should use instead, but this was quick and
	 * easy to build.
	 * 
	 */
	//coordinate inner class
	private class Coordinate {
		public int x;
		public int y;

		//constructor
		public Coordinate(int newX, int newY) {
			x = newX;
			y = newY;
		}

		//override equals
		public boolean equals(Coordinate other) {
			if (x == other.x && y == other.y) {
				return true;
			}
			return false;
		}

		//override toString
		@Override
		public String toString() {
			return "Coordinate: [" + x + "," + y + "]";
		}
	}

}
