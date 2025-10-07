import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// Leitura do mapa a partir de um arquivo de texto
public class Map {
    public static char[][] loadMap(String filePath) {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            java.util.List<char[]> lines = new java.util.ArrayList<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Ignora linhas vazias
                if (line.trim().isEmpty()) {
                    continue;
                }
                lines.add(line.toCharArray());
            }

            // Valida se o mapa está vazio
            if (lines.isEmpty()) {
                System.out.println("Mapa vazio");
                return null;
            }

            // Valida se todas as linhas têm o mesmo tamanho
            int expectedWidth = lines.get(0).length;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).length != expectedWidth) {
                    System.out.println("Erro: linha " + (i+1) + " tem tamanho diferente. Esperado: " + expectedWidth + ", encontrado: " + lines.get(i).length);
                    return null;
                }
            }

            return lines.toArray(new char[lines.size()][]);
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado: " + filePath);
            return null;
        }
    }
}
