import javax.swing.*;

public class Pacman extends JFrame {

    public Pacman() {
        Model model = new Model();
        add(model);
        setTitle("Pacman");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Pacman().setVisible(true));
    }
}
