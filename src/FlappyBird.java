import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    // images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // bird class
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // pipe class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64; // scaled by 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // game logic
    Bird bird;
    int velocityX = -4; // move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; // move bird up/down speed.
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    boolean gameStarted = false;
    double score = 0;
    double bestScore = 0;

    Timer hideBestScoreTimer;
    boolean showBestScore = true; // to hide best score after 3 seconds

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // load images
        backgroundImg = new ImageIcon(getClass().getResource("/flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("/flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("/toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("/bottompipe.png")).getImage();

        // bird
        bird = new Bird(birdImg);
        pipes = new ArrayList<>();

        // place pipes timer
        placePipeTimer = new Timer(1500, e -> placePipes());

        // game timer
        gameLoop = new Timer(1000 / 60, this); // milliseconds between frames

        showStartMenu();
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    void showStartMenu() {
        gameStarted = false;
        pipes.clear();
        velocityY = 0;
        bird.y = birdY;
        gameLoop.stop();
        placePipeTimer.stop();
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // background
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        // bird
        g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);

        // pipes
        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));

        if (gameStarted) {
            g.drawString(String.valueOf((int) score), 10, 35);
        }

        if (gameOver) {
            g.drawString("Game Over: " + (int) score, 10, 35);
        }

        // show best score at the top-right corner
        if (showBestScore && bestScore > 0) {
            g.drawString("Best: " + (int) bestScore, boardWidth - 140, 35);
        }

        // show start/restart message
        if (!gameStarted) {
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            g.drawString("Press any key to start", boardWidth / 6, boardHeight / 2);
        }
    }

    public void move() {
        // bird movement
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); // apply gravity to current bird.y, limit the bird.y to top of the canvas

        // pipes movement
        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5; // score increment for passing a pipe set
                pipe.passed = true;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
                endGame();
            }
        }

        if (score >= 20 && score < 40) {
            velocityX = -5;
        } else if (score >= 40 && score < 60) {
            velocityX = -6;
        } else if (score >= 60) {
            velocityX = -7;
        }

        if (bird.y > boardHeight) {
            gameOver = true;
            endGame();
        }
    }

    void endGame() {
        gameStarted = false;
        if (score > bestScore) {
            bestScore = score; // update best score
        }
        gameLoop.stop();
        placePipeTimer.stop();
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                resetGame();
            }
            return;
        }

        if (!gameStarted) {
            startGame();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9; // bird jump
        }
    }

    void resetGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        gameOver = false;
        score = 0;
        showStartMenu();
        velocityX = -4; // reset speed on game restart
    }

    void startGame() {
        gameStarted = true;
        gameOver = false;
        score = 0;
        gameLoop.start();
        placePipeTimer.start();
        showBestScore = true;

        if (hideBestScoreTimer != null) {
            hideBestScoreTimer.stop();
        }

        hideBestScoreTimer = new Timer(3000, e -> showBestScore = false);
        hideBestScoreTimer.setRepeats(false);
        hideBestScoreTimer.start();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void setHeight(int newHeight) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHeight'");
    }
}
