import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// Leitura do mapa a partir de um arquivo de texto
public class Map {
    public static char[][] loadMap(String filePath) {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            java.util.List<char[]> lines = new java.util.ArrayList<>();
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine().toCharArray());
            }
            return lines.toArray(new char[lines.size()][]);
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado");
            return null;
        }
    }
}

