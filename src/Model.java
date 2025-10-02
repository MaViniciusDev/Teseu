import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Model extends JPanel implements ActionListener {

    private Dimension d;
    private final Font smallFont = new Font("Helvetica", Font.BOLD, 14);
    private boolean inGame = false;
    private boolean dying = false;

    private int BLOCK_SIZE = 24;
    private final int N_BLOCKS = 13;
    private final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE;
    private final int PACMAN_SPEED = 6;

    private int score;
    private int[] dx, dy;

    private Image up, down, left, right;
    private Image food;

    private int pacman_x, pacman_y, pacmand_x, pacmand_y;
    private int req_dx, req_dy;

    private char[][] map = Map.loadMap("src/resources/maze.txt");

    public static short[] mapToLevelData(char[][] map) {
        int rows = map.length;
        int cols = map[0].length;
        short[] levelData = new short[rows*cols];

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                short value = 0;

                //Verifica se é parede
                if (map[y][x] == 'X') {
                    // Esquerda
                    if (x == 0 || map[y][x - 1] == 'X') value |= 1;
                    // Cima
                    if (y == 0 || map[y - 1][x] == 'X') value |= 2;
                    // Direita
                    if (x == cols - 1 || map[y][x + 1] == 'X') value |= 4;
                    // Baixo
                    if (y == rows - 1 || map[y + 1][x] == 'X') value |= 8;
                }

                //comida
                if (map[y][x] == '_' || map[y][x] == 'o') {
                    value |= 16;
                }

                levelData[y*cols+x] = value;

            }

        }

        return levelData;

    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}



