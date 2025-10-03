import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Classe principal do jogo (painel) que gerencia:
 * - Carregamento do mapa a partir de um arquivo (via classe Map)
 * - Representação do labirinto usando bits por célula
 * - Movimentação do Pacman
 * - Coleta de comidas e liberação da saída
 * - Pontuação, vidas e estados de vitória/derrota
 * - Renderização gráfica
 */
public class Model extends JPanel implements ActionListener {

    // Bits para representar propriedades de um bloco (célula) do labirinto:
    // 1 = parede à esquerda
    // 2 = parede acima
    // 4 = parede à direita
    // 8 = parede abaixo
    // 16 = contém comida
    // 32 = é a saída
    private static final int BLOCK_SIZE = 24;            // Tamanho (pixels) de cada bloco
    private static final boolean DEBUG = true;           // Ativa logs de depuração
    private static final boolean LOG_STEPS = false;      // Ativa log detalhado de passos (movimento)
    private static final int PACMAN_SPEED = 3;           // Velocidade (pixels por frame)
    private static final int FOOD_VALUE = 10;            // Pontos ganhos por comida
    private static final int STEP_PENALTY = -1;          // Penalidade ao entrar em um novo bloco
    private static final int WIN_BONUS = 100;            // Bônus ao vencer

    // Máscaras de bits para facilitar leitura
    private static final short LEFT_BIT   = 1;
    private static final short TOP_BIT    = 2;
    private static final short RIGHT_BIT  = 4;
    private static final short BOTTOM_BIT = 8;
    private static final short FOOD_BIT   = 16;
    private static final short EXIT_BIT   = 32;

    private final Font smallFont = new Font("Arial", Font.BOLD, 14); // Fonte para textos na HUD

    // Imagens cacheadas estaticamente para evitar recarregamento
    private static Image DOWN_IMG, UP_IMG, LEFT_IMG, RIGHT_IMG, HEART_IMG, FOOD_IMG;

    // Matriz de caracteres carregada do arquivo do labirinto
    private final char[][] map = initMap();
    private final int ROWS = map.length;             // Número de linhas do mapa
    private final int COLS = map[0].length;          // Número de colunas do mapa
    private final int SCREEN_WIDTH = COLS * BLOCK_SIZE;   // Largura da área de jogo
    private final int SCREEN_HEIGHT = ROWS * BLOCK_SIZE;  // Altura da área de jogo

    // levelData: dados iniciais construídos a partir do mapa (modelo base)
    private final short[] levelData = buildLevelData();
    // screenData: estado mutável durante o jogo (comidas consumidas etc.)
    private final short[] screenData = new short[ROWS * COLS];

    private boolean inGame = false;    // Indica se a partida está em andamento
    private boolean gameWon = false;   // Indica se o jogador venceu

    private int score;                 // Pontuação atual
    private int lives;                 // Vidas restantes

    // Coordenadas do Pacman em pixels
    private int pacman_x, pacman_y;
    // Direção aplicada atualmente
    private int pacmand_x, pacmand_y;
    // Direção solicitada (tecla pressionada)
    private int req_dx, req_dy;

    private int exitRow = -1;
    private boolean exitUnlocked = false;     // Se a saída já foi liberada (todas comidas coletadas)
    private int totalFood = 0;                // Quantidade total de comidas no início
    private int foodsLeft = 0;                // Quantas comidas ainda restam
    private int startRow = -1, startCol = -1; // Posição inicial (E)

    // Imagens instanciadas (referências locais)
    private Image up, down, left, right, heart, food;

    private boolean hungerJustDied = false;   // Usado para exibir mensagem ao morrer por fome (saída antecipada)
    private int frameCounter = 0;             // Contador de frames para debug/memória

    /** Construtor: carrega imagens, inicializa estado e inicia loop do jogo */
    public Model() {
        loadImages();                         // Carrega imagens (uma vez)
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT + 40));
        setFocusable(true);                   // Necessário para receber eventos de teclado
        addKeyListener(new TAdapter());       // Adiciona controle por teclado
        recalcFoodCount(levelData);           // Conta comidas antes de iniciar
        initGame();                           // Prepara variáveis do jogo
        // Timer Swing para repintar e atualizar o jogo
        Timer timer = new Timer(80, this);          // Intervalo ~12.5 FPS
        timer.start();                        // Inicia ciclo
    }

    /** Carrega e cacheia as imagens usadas no jogo */
    private void loadImages() {
        if (DOWN_IMG == null) { // Carrega apenas uma vez
            DOWN_IMG  = new ImageIcon("src/images/down.gif").getImage();
            UP_IMG    = new ImageIcon("src/images/up.gif").getImage();
            LEFT_IMG  = new ImageIcon("src/images/left.gif").getImage();
            RIGHT_IMG = new ImageIcon("src/images/right.gif").getImage();
            HEART_IMG = new ImageIcon("src/images/heart.png").getImage();
            FOOD_IMG  = new ImageIcon("src/images/food.png").getImage();
        }
        // Atribui às referências locais
        down = DOWN_IMG; up = UP_IMG; left = LEFT_IMG; right = RIGHT_IMG; heart = HEART_IMG; food = FOOD_IMG;
    }

    /**
     * Constrói o vetor de shorts (levelData) a partir dos caracteres do mapa.
     * Para cada célula aberta calcula quais paredes existem (derivadas de vizinhos 'X').
     * Marca também onde há comida, saída ou posição inicial.
     */
    private short[] buildLevelData() {
        short[] data = new short[ROWS * COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                char ch = map[r][c];
                if (ch == 'X') {               // Parede sólida (não é espaço de jogo)
                    data[r * COLS + c] = 0;
                    continue;
                }
                short v = 0;
                // Marca paredes conforme presença de 'X' nos vizinhos ou bordas do mapa
                if (c == 0 || map[r][c - 1] == 'X') v |= LEFT_BIT;
                if (r == 0 || map[r - 1][c] == 'X') v |= TOP_BIT;
                if (c == COLS - 1 || map[r][c + 1] == 'X') v |= RIGHT_BIT;
                if (r == ROWS - 1 || map[r + 1][c] == 'X') v |= BOTTOM_BIT;

                // Marca conteúdo especial
                if (ch == 'o') {
                    v |= FOOD_BIT;                 // Comida
                } else if (ch == 'S') {
                    v |= EXIT_BIT;                 // Saída
                    exitRow = r; // Posição da saída (S)
                } else if (ch == 'E') {            // Posição inicial (não é saída)
                    startRow = r; startCol = c;
                }
                data[r * COLS + c] = v;            // Guarda resultado
            }
        }
        return data;
    }

    /** Reconta comidas em um dado array (levelData ou screenData) */
    private void recalcFoodCount(short[] data) {
        int count = 0;
        for (short cell : data) {
            if ((cell & FOOD_BIT) != 0) count++;
        }
        totalFood = count;
        foodsLeft = totalFood;
        if (DEBUG) System.out.println("[DEBUG] Recontagem de comidas: totalFood=" + totalFood);
    }

    /** Inicializa/reinicia o estado da partida */
    private void initGame() {
        lives = 3;
        score = 0;
        gameWon = false;
        hungerJustDied = false;
        System.arraycopy(levelData, 0, screenData, 0, screenData.length); // Copia estado base
        recalcFoodCount(screenData);                                      // Recalcula comidas
        exitUnlocked = false;                                             // Saída começa bloqueada
        if (foodsLeft == 0) {                                             // Caso especial: nenhum alimento
            exitUnlocked = true;
            if (DEBUG) System.out.println("[DEBUG] Nenhuma comida após init. Saída liberada.");
        } else if (DEBUG) {
            System.out.println("[DEBUG] Jogo iniciado. Comidas: " + foodsLeft + ", saída bloqueada.");
        }
        placePacman();                // Define posição inicial
        pacmand_x = pacmand_y = 0;    // Zera movimentação
        req_dx = req_dy = 0;          // Zera direção solicitada
    }

    /** Posiciona o Pacman: primeiro tenta 'E', senão escolhe qualquer célula válida */
    private void placePacman() {
        if (startRow >= 0 && startCol >= 0) { // Se posição 'E' foi encontrada
            pacman_x = startCol * BLOCK_SIZE;
            pacman_y = startRow * BLOCK_SIZE;
            if (DEBUG) System.out.println("[DEBUG] Pacman iniciado em E: ("+startRow+","+startCol+")");
            return;
        }
        // Fallback: primeira célula não parede e não saída
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (map[r][c] != 'X' && (screenData[r * COLS + c] & EXIT_BIT) == 0) {
                    pacman_x = c * BLOCK_SIZE;
                    pacman_y = r * BLOCK_SIZE;
                    if (DEBUG) System.out.println("[DEBUG] Pacman iniciado fallback em ("+r+","+c+")");
                    return;
                }
            }
        }
        // Se nada encontrado (mapa inválido) posiciona no zero
        pacman_x = pacman_y = 0;
    }

    /** Loop de jogo: chamada a cada repaint quando inGame */
    private void playGame(Graphics2D g2d) {
        if (!gameWon) movePacman();  // Processa lógica de movimento/colisão
        drawPacman(g2d);             // Desenha o Pacman
        drawExitStatus(g2d);         // Informações sobre a saída
    }

    /** Carrega o mapa do arquivo usando a classe Map; fallback se falhar */
    private char[][] initMap() {
        char[][] m = Map.loadMap("src/resources/maze.txt");
        if (m == null || m.length == 0) { // Falha ou vazio
            if (DEBUG) System.out.println("[DEBUG] Mapa não carregado. Usando fallback.");
            String[] fallback = {
                    "XXXX",
                    "XEXX",
                    "XoSX",
                    "XXXX"
            };
            char[][] fb = new char[fallback.length][];
            for (int i=0;i<fallback.length;i++) fb[i] = fallback[i].toCharArray();
            return fb;
        }
        return m;
    }

    /**
     * Lógica de movimento do Pacman:
     * - Verifica se está alinhado em bloco para processar mudança de direção
     * - Coleta comida quando presente
     * - Controla morte por tentar sair antes de liberar
     * - Aplica penalidade de passo ao entrar em novo bloco
     * - Verifica vitória ao entrar na saída liberada
     */
    private void movePacman() {
        int prevCellCol = pacman_x / BLOCK_SIZE;  // Coluna anterior (antes de mover)
        int prevCellRow = pacman_y / BLOCK_SIZE;  // Linha anterior

        // Só processa paredes e coleta quando está centralizado num bloco
        if (pacman_x % BLOCK_SIZE == 0 && pacman_y % BLOCK_SIZE == 0) {
            int col = pacman_x / BLOCK_SIZE;
            int row = pacman_y / BLOCK_SIZE;
            int pos = row * COLS + col;
            short cell = screenData[pos];

            // Se está em uma saída bloqueada -> morte por fome
            if ((cell & EXIT_BIT) != 0 && !exitUnlocked) {
                hungerDeath();
                return; // Não processa mais este frame
            }

            // Coleta comida se houver
            if ((cell & FOOD_BIT) != 0) {
                screenData[pos] = (short)(cell & ~FOOD_BIT); // Remove bit de comida
                if (foodsLeft > 0) foodsLeft--;               // Decrementa contador
                if (foodsLeft <= 0) {                        // Todas comidas coletadas
                    foodsLeft = 0;
                    if (!exitUnlocked) {
                        exitUnlocked = true;                 // Libera saída
                        if (DEBUG) System.out.println("[DEBUG] Todas as comidas coletadas. Saída liberada!");
                    }
                }
                score += FOOD_VALUE;                          // Atualiza pontuação
                if (DEBUG) System.out.println("[DEBUG] Comida coletada. Restam: " + foodsLeft);
            }

            // Tenta aplicar direção requisitada (teclas) se não houver parede
            if (req_dx != 0 || req_dy != 0) {
                int nextRow = row + req_dy;
                int nextCol = col + req_dx;
                boolean canApply = true;
                if (nextRow >= 0 && nextRow < ROWS && nextCol >=0 && nextCol < COLS) {
                    int nPos = nextRow * COLS + nextCol;
                    short nextCell = screenData[nPos];
                    // Se a próxima célula é saída bloqueada -> morre
                    if ((nextCell & EXIT_BIT) != 0 && !exitUnlocked) {
                        hungerDeath();
                        return;
                    }
                    // Verifica paredes em relação à direção solicitada
                    if ((req_dx == -1 && (cell & LEFT_BIT) != 0) ||
                        (req_dx == 1  && (cell & RIGHT_BIT) != 0) ||
                        (req_dy == -1 && (cell & TOP_BIT) != 0) ||
                        (req_dy == 1  && (cell & BOTTOM_BIT) != 0)) {
                        canApply = false; // Parede bloqueia troca de direção
                    }
                }
                if (canApply) { // Aplica direção válida
                    pacmand_x = req_dx; pacmand_y = req_dy;
                }
            }

            // Verifica se direção atual bate numa parede e cancela se necessário
            int targetRow = row + pacmand_y;
            int targetCol = col + pacmand_x;
            if ((pacmand_x == -1 && (cell & LEFT_BIT) != 0) ||
                (pacmand_x == 1  && (cell & RIGHT_BIT) != 0) ||
                (pacmand_y == -1 && (cell & TOP_BIT) != 0) ||
                (pacmand_y == 1  && (cell & BOTTOM_BIT) != 0)) {
                // Apenas loga se estava tentando mover
                if (DEBUG) System.out.println("[DEBUG] Movimento cancelado (parede)." );
                pacmand_x = pacmand_y = 0; // Para o movimento
            } else if (targetRow >=0 && targetRow < ROWS && targetCol >=0 && targetCol < COLS) {
                int tPos = targetRow * COLS + targetCol;
                short tCell = screenData[tPos];
                if ((tCell & EXIT_BIT) != 0 && !exitUnlocked) { // Se destino é saída bloqueada
                    hungerDeath();
                    return;
                }
            }

            // Vitória: entrou na saída depois de desbloqueada
            if ((cell & EXIT_BIT) != 0 && exitUnlocked) {
                onGameWon();
            }
        }
        // Move em pixels de acordo com direção atual
        pacman_x += PACMAN_SPEED * pacmand_x;
        pacman_y += PACMAN_SPEED * pacmand_y;

        // Penalidade de passo: só quando realmente entrou em outra célula
        int newCellCol = pacman_x / BLOCK_SIZE;
        int newCellRow = pacman_y / BLOCK_SIZE;
        if ((pacmand_x != 0 || pacmand_y != 0) && (newCellCol != prevCellCol || newCellRow != prevCellRow)) {
            score += STEP_PENALTY;
            if (DEBUG && LOG_STEPS) System.out.println("[DEBUG] Passo: score=" + score);
        }
        // Debug periódico de memória
        if (DEBUG) {
            frameCounter++;
            if (frameCounter % 300 == 0) logMemory();
        }
    }

    /** Processa morte por tentar sair sem coletar todas as comidas */
    private void hungerDeath() {
        lives--;                         // Perde uma vida
        hungerJustDied = true;           // Marca para exibir mensagem específica
        if (DEBUG) System.out.println("[DEBUG] Morte por fome! Vidas restantes: " + lives);
        if (lives <= 0) {                // Game over
            inGame = false;
            gameWon = false;
        } else {                         // Reinicia posição
            placePacman();
        }
        pacmand_x = pacmand_y = 0;
        req_dx = req_dy = 0;
    }

    /** Marca estado de vitória e aplica bônus */
    private void onGameWon() {
        if (!gameWon) {
            if (DEBUG) System.out.println("[DEBUG] Vitória: foodsLeft="+foodsLeft+", exitUnlocked="+exitUnlocked);
            score += WIN_BONUS;          // Aplica bônus final
            gameWon = true;
            inGame = false;              // Pausa o jogo
        }
    }

    /** Desenha o Pacman conforme direção solicitada mais recente */
    private void drawPacman(Graphics2D g2d) {
        Image img = down;
        if (req_dx == -1) img = left;
        else if (req_dx == 1) img = right;
        else if (req_dy == -1) img = up;
        g2d.drawImage(img, pacman_x + 1, pacman_y + 1, this);
    }

    /** Desenha o labirinto: paredes, comidas e saída */
    private void drawMaze(Graphics2D g2d) {
        int idx = 0; // Índice linear da célula
        g2d.setStroke(new BasicStroke(5));
        for (int r = 0; r < ROWS; r++) {
            int y = r * BLOCK_SIZE;
            for (int c = 0; c < COLS; c++) {
                int x = c * BLOCK_SIZE;
                char raw = map[r][c];
                short cell = screenData[idx];
                if (raw != 'X') { // Não desenha interior de paredes sólidas
                    g2d.setColor(new Color(0,72,251));
                    // Desenha bordas conforme bits
                    if ((cell & LEFT_BIT) != 0)   g2d.drawLine(x, y, x, y + BLOCK_SIZE - 1);
                    if ((cell & TOP_BIT) != 0)    g2d.drawLine(x, y, x + BLOCK_SIZE - 1, y);
                    if ((cell & RIGHT_BIT) != 0)  g2d.drawLine(x + BLOCK_SIZE - 1, y, x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1);
                    if ((cell & BOTTOM_BIT) != 0) g2d.drawLine(x, y + BLOCK_SIZE - 1, x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1);
                    // Desenha comida (imagem) se presente
                    if ((cell & FOOD_BIT) != 0) {
                        g2d.drawImage(food, x + 4, y + 4, BLOCK_SIZE - 8, BLOCK_SIZE - 8, this);
                    }
                    // Desenha saída (bloqueada = vermelho escuro, liberada = vermelho vivo com borda amarela)
                    if ((cell & EXIT_BIT) != 0) {
                        g2d.setColor(exitUnlocked ? new Color(255,0,0) : new Color(110,0,0));
                        g2d.fillRect(x + 2, y + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                        if (exitUnlocked) {
                            g2d.setColor(Color.YELLOW);
                            g2d.drawRect(x + 2, y + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
                        }
                    }
                }
                idx++;
            }
        }
    }

    /** Exibe mensagens sobre saída, morte por fome, vitória ou instruções */
    private void drawExitStatus(Graphics2D g2d) {
        if (exitRow >= 0) { // Só mostra algo se há saída definida
            g2d.setFont(smallFont);
            if (!inGame && lives <=0 && !gameWon) { // Game Over
                g2d.setColor(Color.red);
                g2d.drawString("Game Over - Morreu de fome!", 200, SCREEN_HEIGHT + 20);
                g2d.setColor(Color.yellow);
                g2d.drawString("Press SPACE para reiniciar", 200, SCREEN_HEIGHT + 38);
                return;
            }
            if (hungerJustDied) { // Mensagem após morte por tentar sair cedo
                g2d.setColor(Color.orange);
                g2d.drawString("Você tentou sair sem comer tudo!", 200, SCREEN_HEIGHT + 20);
                g2d.setColor(Color.gray);
                g2d.drawString("Resto: " + foodsLeft + " comidas", 200, SCREEN_HEIGHT + 38);
                hungerJustDied = false; // Reseta flag (mensagem aparece uma vez)
                return;
            }
            g2d.setColor(exitUnlocked ? new Color(255,80,80) : Color.gray);
            String msg = exitUnlocked ? "Saida liberada!" : "Colete todas as comidas";
            g2d.drawString(msg, 200, SCREEN_HEIGHT + 20);
            if (gameWon) {
                g2d.setColor(Color.yellow);
                g2d.drawString("Venceu! Score +" + WIN_BONUS, 200, SCREEN_HEIGHT + 38);
            }
        }
    }

    /** Desenha HUD: score, comidas restantes e vidas */
    private void drawScore(Graphics2D g) {
        g.setFont(smallFont);
        g.setColor(new Color(5,181,79));
        g.drawString("Score: " + score, 10, SCREEN_HEIGHT + 20);
        g.drawString("Restam: " + foodsLeft + "/" + totalFood, 10, SCREEN_HEIGHT + 38);
        // Desenha corações representando vidas
        for (int i = 0; i < lives; i++) {
            g.drawImage(heart, SCREEN_WIDTH - (i + 1) * 30, SCREEN_HEIGHT + 4, this);
        }
    }

    /** Tela inicial antes do jogo começar */
    private void showIntro(Graphics2D g2d) {
        g2d.setColor(Color.yellow);
        g2d.drawString("Press SPACE para iniciar", 20, SCREEN_HEIGHT / 2);
        g2d.drawString("Comidas: " + totalFood, 20, SCREEN_HEIGHT / 2 + 20);
    }

    /** Ciclo de renderização do Swing */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT + 40); // Fundo
        drawMaze(g2d);      // Labirinto
        drawScore(g2d);     // HUD
        if (inGame) playGame(g2d); else showIntro(g2d); // Estado do jogo
        Toolkit.getDefaultToolkit().sync(); // Sincroniza (melhora suavidade em alguns SOs)
    }

    /** Listener de teclado para controlar movimento e iniciar/reiniciar o jogo */
    private class TAdapter extends KeyAdapter {
        @Override public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (inGame) { // Enquanto o jogo está rodando
                if (k == KeyEvent.VK_LEFT)  { req_dx = -1; req_dy = 0; }
                else if (k == KeyEvent.VK_RIGHT){ req_dx = 1; req_dy = 0; }
                else if (k == KeyEvent.VK_UP)   { req_dx = 0; req_dy = -1; }
                else if (k == KeyEvent.VK_DOWN) { req_dx = 0; req_dy = 1; }
                else if (k == KeyEvent.VK_ESCAPE) inGame = false; // Pausa / sai para intro
            } else if (k == KeyEvent.VK_SPACE) { // Espaço inicia/reinicia
                inGame = true;
                initGame();
            }
        }
    }

    /** Handler do Timer: apenas repinta */
    @Override public void actionPerformed(ActionEvent e) { repaint(); }

    /** Log simples de memória usada para depuração */
    private void logMemory() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        System.out.println("[DEBUG] Memória usada: " + used + " MB");
    }
}
