import java.awt.Point;
import java.util.*;

// Agente inteligente para o Pacman
public class Agent {
    private final Model model;
    private final int ROWS;
    private final int COLS;

    // Conhecimento
    private final char[][] knowledge;
    private final boolean[][] visited;

    // Planejamento
    private final Queue<Point> currentPath;
    private int foodsCollected = 0;
    private static final int TOTAL_FOOD = 4;

    // Direções
    private static final int[] DX = {0, 0, 1, -1};
    private static final int[] DY = {-1, 1, 0, 0};

    public Agent(Model model) {
        this.model = model;
        this.ROWS = model.getRows();
        this.COLS = model.getCols();
        this.knowledge = new char[ROWS][COLS];
        this.visited = new boolean[ROWS][COLS];
        this.currentPath = new LinkedList<>();

        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(knowledge[r], '?');
        }
    }

    public void reset() {
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(knowledge[r], '?');
            Arrays.fill(visited[r], false);
        }
        currentPath.clear();
        foodsCollected = 0;
    }

    public Point decideNextMove() {
        int row = model.getPacmanRow();
        int col = model.getPacmanCol();

        // Atualiza conhecimento
        updateKnowledge(row, col);
        visited[row][col] = true;

        // Se precisa de novo caminho
        if (currentPath.isEmpty()) {
            planPath(row, col);
        }

        return followPath(row, col);
    }

    private void updateKnowledge(int row, int col) {
        char[][] sensor = model.getSensor();

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = row + dr;
                int c = col + dc;
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    int sr = dr + 1;
                    int sc = dc + 1;
                    if (sr == 2 && sc == 2) continue;
                    char sensed = sensor[sr][sc];
                    if (sensed != '?') knowledge[r][c] = sensed;
                }
            }
        }

        knowledge[row][col] = sensor[1][1];

        // Conta comidas coletadas
        if (knowledge[row][col] == '_' && !visited[row][col]) {
            // Pode ter coletado comida
        }
    }

    private void planPath(int row, int col) {
        currentPath.clear();

        // Se já coletou as comidas, vai para saída
        if (foodsCollected >= TOTAL_FOOD || model.isExitUnlocked()) {
            Point exit = findExit();
            if (exit != null) {
                List<Point> path = aStar(row, col, exit.y, exit.x);
                if (path != null) {
                    currentPath.addAll(path);
                    System.out.println("[AGENT] Indo para saída! (" + path.size() + " passos)");
                    return;
                }
            }
        }

        // Senão, busca comida conhecida
        Point food = findNearestFood(row, col);
        if (food != null) {
            List<Point> path = aStar(row, col, food.y, food.x);
            if (path != null) {
                currentPath.addAll(path);
                System.out.println("[AGENT] Indo buscar comida em (" + food.y + "," + food.x + ")");
                foodsCollected++;
                return;
            }
        }

        // Senão, explora (vai para fronteira)
        Point frontier = findNearestFrontier(row, col);
        if (frontier != null) {
            List<Point> path = aStar(row, col, frontier.y, frontier.x);
            if (path != null) {
                currentPath.addAll(path);
                System.out.println("[AGENT] Explorando...");
            }
        }
    }

    private Point followPath(int row, int col) {
        if (currentPath.isEmpty()) {
            return new Point(0, 0);
        }

        Point next = currentPath.peek();

        if (next.y == row && next.x == col) {
            currentPath.poll();
            if (currentPath.isEmpty()) return new Point(0, 0);
            next = currentPath.peek();
        }

        if (!canMove(row, col, next.y, next.x)) {
            currentPath.clear();
            return new Point(0, 0);
        }

        return new Point(Integer.compare(next.x, col), Integer.compare(next.y, row));
    }

    private List<Point> aStar(int startRow, int startCol, int goalRow, int goalCol) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        HashMap<Point, Node> allNodes = new HashMap<>();
        Set<Point> closed = new HashSet<>();

        Point start = new Point(startCol, startRow);
        Point goal = new Point(goalCol, goalRow);

        Node startNode = new Node(start, null, 0, dist(start, goal));
        open.add(startNode);
        allNodes.put(start, startNode);

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed.contains(cur.pos)) continue;
            closed.add(cur.pos);

            if (cur.pos.equals(goal)) {
                return buildPath(cur);
            }

            for (int i = 0; i < 4; i++) {
                int nr = cur.pos.y + DY[i];
                int nc = cur.pos.x + DX[i];
                Point np = new Point(nc, nr);

                if (closed.contains(np)) continue;
                if (!isWalkable(nr, nc)) continue;
                if (!canMove(cur.pos.y, cur.pos.x, nr, nc)) continue;

                double g = cur.g + 1;
                Node neighbor = allNodes.get(np);

                if (neighbor == null) {
                    neighbor = new Node(np, null, Double.MAX_VALUE, Double.MAX_VALUE);
                    allNodes.put(np, neighbor);
                }

                if (g < neighbor.g) {
                    neighbor.parent = cur;
                    neighbor.g = g;
                    neighbor.f = g + dist(np, goal);
                    open.add(neighbor);
                }
            }
        }

        return null;
    }

    private List<Point> buildPath(Node goal) {
        LinkedList<Point> path = new LinkedList<>();
        Node cur = goal;
        while (cur.parent != null) {
            path.addFirst(cur.pos);
            cur = cur.parent;
        }
        return path;
    }

    private double dist(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private Point findNearestFood(int row, int col) {
        Point best = null;
        double minDist = Double.MAX_VALUE;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (knowledge[r][c] == 'o') {
                    double d = Math.abs(r - row) + Math.abs(c - col);
                    if (d < minDist) {
                        minDist = d;
                        best = new Point(c, r);
                    }
                }
            }
        }

        return best;
    }

    private Point findExit() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (knowledge[r][c] == 'S') {
                    return new Point(c, r);
                }
            }
        }
        return null;
    }

    private Point findNearestFrontier(int row, int col) {
        Point best = null;
        double minDist = Double.MAX_VALUE;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (isFrontier(r, c)) {
                    double d = Math.abs(r - row) + Math.abs(c - col);
                    if (d < minDist) {
                        minDist = d;
                        best = new Point(c, r);
                    }
                }
            }
        }

        return best;
    }

    private boolean isFrontier(int r, int c) {
        if (!isWalkable(r, c)) return false;

        for (int i = 0; i < 4; i++) {
            int nr = r + DY[i];
            int nc = c + DX[i];
            if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS) {
                if (knowledge[nr][nc] == '?' && canMove(r, c, nr, nc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWalkable(int r, int c) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return false;
        char cell = knowledge[r][c];
        if (cell == '?' || cell == 'X') return false;
        if (cell == 'S' && !model.isExitUnlocked()) return false;
        return true;
    }

    private boolean canMove(int r1, int c1, int r2, int c2) {
        return model.canMoveBetween(r1, c1, r2, c2);
    }

    private static class Node implements Comparable<Node> {
        Point pos;
        Node parent;
        double g, f;

        Node(Point pos, Node parent, double g, double f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.f, o.f);
        }
    }
}

