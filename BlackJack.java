import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class BlackJack {

    // Clase interna para representar una carta
    private class Card {
        String value;
        String type;

        Card(String value, String type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return value + "-" + type;
        }

        // Devuelve el valor numérico de la carta
        public int getValue() {
            if ("AJQK".contains(value)) {
                if ("A".equals(value)) {
                    return 11;
                }
                return 10;
            }
            return Integer.parseInt(value);
        }

        // Indica si la carta es un As
        public boolean isAce() {
            return "A".equals(value);
        }

        // Devuelve la ruta de la imagen de la carta
        public String getImagePath() {
            return "cards/" + toString() + ".png";
        }
    }

    // Variables del juego
    ArrayList<Card> deck; // Mazo de cartas
    Random random = new Random();

    Card hiddenCard; // Carta oculta del dealer
    ArrayList<Card> dealerHand; // Mano del dealer
    int dealerSum; // Suma de la mano del dealer
    int dealerAceCount; // Cantidad de Ases del dealer

    ArrayList<ArrayList<Card>> playersHands = new ArrayList<>(); // Manos de los jugadores
    ArrayList<Integer> playersSums = new ArrayList<>(); // Sumas de los jugadores
    ArrayList<Integer> playersAceCounts = new ArrayList<>(); // Ases de los jugadores
    ArrayList<String> playerNames = new ArrayList<>(); // Nombres de los jugadores
    int currentPlayer = 0; // Jugador actual

    HashMap<String, Integer> victorias = new HashMap<>();

    int boardWidth = 1000;
    int boardHeight = 1000;

    int cardWidth = 110;
    int cardHeight;

    // Componentes de la interfaz gráfica
    JFrame frame = new JFrame("Black Jack");
    JPanel mainMenuPanel = new JPanel();
    JPanel gamePanel = new JPanel() {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(53, 101, 77));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 18));

            // Mostrar siempre la etiqueta "Dealer:" y, si terminó la ronda, también los puntos
            int dealerBlockYOffset = 40; // Espacio superior para los puntos del dealer
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.setColor(Color.YELLOW);

            String dealerLabel = "Dealer:";
            if (!stayButton.isEnabled() && currentPlayer == playersHands.size()) {
                int dealerFinalSum = dealerSum;
                int aceCount = dealerAceCount;
                while (dealerFinalSum > 21 && aceCount > 0) {
                    dealerFinalSum -= 10;
                    aceCount--;
                }
                dealerLabel += " " + dealerFinalSum + " puntos";
            }
            g.drawString(dealerLabel, 20, dealerBlockYOffset);

            // Dibuja la carta oculta del dealer (ajustada hacia abajo)
            int dealerCardsYOffset = dealerBlockYOffset + 10;
            Image hiddenCardImg = new ImageIcon("cards/BACK.png").getImage();
            if (!stayButton.isEnabled()) {
                hiddenCardImg = new ImageIcon(hiddenCard.getImagePath()).getImage();
            }
            g.drawImage(hiddenCardImg, 20, dealerCardsYOffset, cardWidth, cardHeight, null);

            // Dibuja las cartas del dealer (ajustadas hacia abajo)
            for (int i = 0; i < dealerHand.size(); i++) {
                Card card = dealerHand.get(i);
                Image cardImg = new ImageIcon(card.getImagePath()).getImage();
                g.drawImage(cardImg, cardWidth + 25 + (cardWidth + 5) * i, dealerCardsYOffset, cardWidth, cardHeight, null);
            }

            // Dibuja las cartas y resultados de cada jugador (todo más abajo)
            int playersYOffsetBase = dealerCardsYOffset + cardHeight + 40; // Baja todo el bloque de jugadores
            for (int p = 0; p < playersHands.size(); p++) {
                ArrayList<Card> hand = playersHands.get(p);
                int yOffset = playersYOffsetBase + p * (cardHeight + 40);
                for (int i = 0; i < hand.size(); i++) {
                    Card card = hand.get(i);
                    Image cardImg = new ImageIcon(card.getImagePath()).getImage();
                    g.drawImage(cardImg, 20 + (cardWidth + 5) * i, yOffset, cardWidth, cardHeight, null);
                }

                g.setFont(new Font("Arial", Font.BOLD, 16));
                String result = "";
                Color resultColor = Color.WHITE;

                // Muestra el resultado y puntos al final del juego
                if (!stayButton.isEnabled() && currentPlayer == playersHands.size()) {
                    int playerSum = playersSums.get(p);
                    int aceCount = playersAceCounts.get(p);
                    while (playerSum > 21 && aceCount > 0) {
                        playerSum -= 10;
                        aceCount--;
                    }
                    result = evaluateWinner(playerSum);

                    if (result.contains("Ganaste")) {
                        resultColor = new Color(0, 200, 0);
                    } else if (result.contains("Perdiste")) {
                        resultColor = Color.RED;
                    } else {
                        resultColor = Color.YELLOW;
                    }
                    result = " - " + result + " (" + playerSum + " puntos)";
                } else if (!stayButton.isEnabled() || (p < currentPlayer)) {
                    // Si el jugador ya se plantó, muestra sus puntos
                    int playerSum = playersSums.get(p);
                    int aceCount = playersAceCounts.get(p);
                    while (playerSum > 21 && aceCount > 0) {
                        playerSum -= 10;
                        aceCount--;
                    }
                    result = " (" + playerSum + " puntos)";
                }

                g.setColor(resultColor);
                g.drawString(playerNames.get(p) + (p == currentPlayer && stayButton.isEnabled() ? " ←" : "") + result, 20, yOffset - 10);
            }

            // Mensaje de fin del juego (ajustado hacia abajo)
            if (!stayButton.isEnabled() && currentPlayer == playersHands.size()) {
                int yOffset = playersYOffsetBase + playersHands.size() * (cardHeight + 40);
                g.setFont(new Font("Arial", Font.BOLD, 32));
                g.setColor(Color.WHITE);
                g.drawString("Fin del juego.", 220, yOffset);
            }
        }
    };
    JPanel buttonPanel = new JPanel();
    JButton hitButton = new JButton("Otra");
    JButton stayButton = new JButton("Parar");
    JButton startButton = new JButton("Jugar");
    JButton restartButton = new JButton("Menú Principal");
    JButton rematchButton = new JButton("Otra Partida");

    //  Música
    Clip backgroundClip;
    boolean isMusicPlaying = true;
    JButton toggleMusicButton = new JButton("Pausar Música");

    // Constructor principal
    public BlackJack() {
        this.cardHeight = 154;
        setupMenu();
        playBackgroundMusic();
    }

    // Configura el menú principal
    private void setupMenu() {
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainMenuPanel.setLayout(new BorderLayout());
        JLabel title = new JLabel("Bienvenido a BlackJack", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        mainMenuPanel.setBackground(new Color(53, 101, 77));
        title.setForeground(Color.WHITE);
        mainMenuPanel.add(title, BorderLayout.CENTER);

        startButton.setFont(new Font("Arial", Font.PLAIN, 20));
        startButton.setFocusable(false);
        toggleMusicButton.setFont(new Font("Arial", Font.PLAIN, 20));
        toggleMusicButton.setFocusable(false);

        toggleMusicButton.addActionListener(e -> toggleMusic());
        startButton.addActionListener(e -> showPlayerCountSelection());

        JPanel buttonMenuPanel = new JPanel(new GridLayout(2, 1));
        buttonMenuPanel.add(startButton);
        buttonMenuPanel.add(toggleMusicButton);

        mainMenuPanel.add(buttonMenuPanel, BorderLayout.SOUTH);
        frame.setContentPane(mainMenuPanel);
        frame.setVisible(true);
    }

    // Reproduce la música de fondo en bucle
    @SuppressWarnings("UseSpecificCatch")
    private void playBackgroundMusic() {
        try {
            File audioFile = new File("src/musica/instrumental.wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioStream);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "No se pudo reproducir la música: " + e.getMessage());
        }
    }

    // Pausa o reanuda la música de fondo
    private void toggleMusic() {
        if (backgroundClip != null) {
            if (isMusicPlaying) {
                backgroundClip.stop();
                toggleMusicButton.setText("Reanudar Música");
            } else {
                backgroundClip.start();
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
                toggleMusicButton.setText("Pausar Música");
            }
            isMusicPlaying = !isMusicPlaying;
        }
    }

    // Muestra la selección de cantidad de jugadores
    private void showPlayerCountSelection() {
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new GridLayout(6, 1));
        JLabel question = new JLabel("¿Cuántos jugadores van a ser?", SwingConstants.CENTER);
        question.setFont(new Font("Arial", Font.BOLD, 20));
        selectionPanel.add(question);
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] buttons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            buttons[i] = new JRadioButton((i + 1) + " jugador(es)");
            group.add(buttons[i]);
            selectionPanel.add(buttons[i]);
        }
        JButton confirm = new JButton("Confirmar");
        selectionPanel.add(confirm);
        frame.setContentPane(selectionPanel);
        frame.revalidate();

        confirm.addActionListener(e -> {
            int count = 1;
            for (int i = 0; i < 4; i++) {
                if (buttons[i].isSelected()) {
                    count = i + 1;
                    break;
                }
            }
            setupPlayers(count);
        });
    }

    // Pide los nombres de los jugadores y comienza el juego
    private void setupPlayers(int count) {
        playerNames.clear();
        victorias.clear(); // Reinicia el recuento al cambiar de jugadores
        for (int i = 0; i < count; i++) {
            String name;
            while (true) {
                name = JOptionPane.showInputDialog(frame, "Nombre del Jugador " + (i + 1) + ":");
                if (name == null) {
                    // Si el usuario cancela, vuelve a la selección de cantidad de jugadores y sale del método
                    showPlayerCountSelection();
                    return;
                }
                if (name.isEmpty()) name = "Jugador " + (i + 1);
                if (playerNames.contains(name)) {
                    JOptionPane.showMessageDialog(frame, "Ese nombre ya está en uso. Elige otro nombre.");
                } else {
                    break;
                }
            }
            playerNames.add(name);
            victorias.put(name, 0); // Inicializa en 0
        }
        startGame();
        renderGameScreen();
    }

    // Dibuja la pantalla principal del juego
    private void renderGameScreen() {
        frame.getContentPane().removeAll();
        frame.setLayout(new BorderLayout());

        gamePanel.setLayout(new BorderLayout());
        gamePanel.setBackground(new Color(53, 101, 77));
        frame.add(gamePanel);

        buttonPanel.removeAll();
        hitButton.setFocusable(false);
        stayButton.setFocusable(false);
        rematchButton.setFocusable(false);
        restartButton.setFocusable(false);

        buttonPanel.add(hitButton);
        buttonPanel.add(stayButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        hitButton.setEnabled(true);
        stayButton.setEnabled(true);

        // Limpia listeners previos para evitar duplicados
        for (ActionListener al : hitButton.getActionListeners()) hitButton.removeActionListener(al);
        for (ActionListener al : stayButton.getActionListeners()) stayButton.removeActionListener(al);
        for (ActionListener al : rematchButton.getActionListeners()) rematchButton.removeActionListener(al);
        for (ActionListener al : restartButton.getActionListeners()) restartButton.removeActionListener(al);

        // Acción de pedir carta
        hitButton.addActionListener(e -> {
            if (currentPlayer >= playersHands.size()) return;

            ArrayList<Card> hand = playersHands.get(currentPlayer);
            int sum = playersSums.get(currentPlayer);
            int aceCount = playersAceCounts.get(currentPlayer);

            Card card = deck.remove(deck.size() - 1);
            sum += card.getValue();
            aceCount += card.isAce() ? 1 : 0;
            hand.add(card);

            while (sum > 21 && aceCount > 0) {
                sum -= 10;
                aceCount--;
            }

            playersSums.set(currentPlayer, sum);
            playersAceCounts.set(currentPlayer, aceCount);

            if (sum > 21) {
                nextPlayer();
            }
            gamePanel.repaint();
        });

        // Acción de plantarse
        stayButton.addActionListener(e -> {
            nextPlayer();
            gamePanel.repaint();
        });

        frame.revalidate();
        frame.repaint();
    }

    // Cambia al siguiente jugador o termina la ronda
    private void nextPlayer() {
        if (currentPlayer < playerNames.size() - 1) {
            currentPlayer++;
        } else {
            currentPlayer++;
            hitButton.setEnabled(false);
            stayButton.setEnabled(false);
            dealerTurn();
            // Evaluar si algún jugador ganó
            boolean alguienGano = false;
            for (int i = 0; i < playersSums.size(); i++) {
                String resultado = evaluateWinner(playersSums.get(i));
                if (resultado.contains("Ganaste")) {
                    alguienGano = true;
                    break;
                }
            }
            if (alguienGano) {
                showVictoryScreen();
            } else {
                showLoseScreen();
            }
        }
    }

    // Muestra opciones al terminar la partida
    private void showResultOptions() {
        buttonPanel.add(rematchButton);
        buttonPanel.add(restartButton);
        frame.revalidate();
        frame.repaint();

        rematchButton.addActionListener(e -> {
            currentPlayer = 0;
            startGame();
            renderGameScreen();
        });

        restartButton.addActionListener(e -> {
            frame.getContentPane().removeAll();
            setupMenu();
        });
    }

    // Inicializa el mazo y reparte cartas
    public void startGame() {
        currentPlayer = 0;

        // Ajusta el tamaño de las cartas según la cantidad de jugadores
        if (playerNames.size() == 1) {
            cardWidth = 180;
            cardHeight = 236;
        } else if (playerNames.size() == 2) {
            cardWidth = 160;
            cardHeight = 208;
        } else if (playerNames.size() == 3) {
            cardWidth = 120;
            cardHeight = 160;
        } else { // 4 o más jugadores
            cardWidth = 90;
            cardHeight = 129;
        }

        buildDeck();
        shuffleDeck();

        dealerHand = new ArrayList<>();
        dealerSum = 0;
        dealerAceCount = 0;

        hiddenCard = deck.remove(deck.size() - 1);
        dealerSum += hiddenCard.getValue();
        dealerAceCount += hiddenCard.isAce() ? 1 : 0;

        Card card = deck.remove(deck.size() - 1);
        dealerSum += card.getValue();
        dealerAceCount += card.isAce() ? 1 : 0;
        dealerHand.add(card);

        playersHands.clear();
        playersSums.clear();
        playersAceCounts.clear();

        for (int i = 0; i < playerNames.size(); i++) {
            ArrayList<Card> hand = new ArrayList<>();
            int sum = 0;
            int aceCount = 0;
            for (int j = 0; j < 2; j++) {
                card = deck.remove(deck.size() - 1);
                sum += card.getValue();
                aceCount += card.isAce() ? 1 : 0;
                hand.add(card);
            }
            playersHands.add(hand);
            playersSums.add(sum);
            playersAceCounts.add(aceCount);
        }
    }

    // Construye el mazo de cartas
    public void buildDeck() {
        deck = new ArrayList<>();
        String[] values = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        String[] types = {"C", "D", "H", "S"};

        for (String type : types) {
            for (String value : values) {
                deck.add(new Card(value, type));
            }
        }
    }

    // Mezcla el mazo de cartas
    public void shuffleDeck() {
        for (int i = 0; i < deck.size(); i++) {
            int j = random.nextInt(deck.size());
            Card temp = deck.get(i);
            deck.set(i, deck.get(j));
            deck.set(j, temp);
        }
    }

    // Evalúa el resultado de un jugador contra el dealer
    public String evaluateWinner(int playerSum) {
        int dealerFinalSum = dealerSum;
        int aceCount = dealerAceCount;
        while (dealerFinalSum > 21 && aceCount > 0) {
            dealerFinalSum -= 10;
            aceCount--;
        }
        if (playerSum > 21) {
            return "Perdiste";
        } else if (dealerFinalSum > 21 || playerSum > dealerFinalSum) {
            return "Ganaste";
        } else if (playerSum < dealerFinalSum) {
            return "Perdiste";
        } else {
            return "Empate";
        }
    }

    // Turno del dealer: saca cartas hasta sumar al menos 17
    public void dealerTurn() {
        while (dealerSum < 17) {
            Card card = deck.remove(deck.size() - 1);
            dealerHand.add(card);
            dealerSum += card.getValue();
            dealerAceCount += card.isAce() ? 1 : 0;

            while (dealerSum > 21 && dealerAceCount > 0) {
                dealerSum -= 10;
                dealerAceCount--;
            }
        }
    }

    // Muestra la pantalla de victoria con los nombres de los ganadores
    private void showVictoryScreen() {
        Timer timer = new Timer(2500, e -> { // <-- Cambiado a 2500 ms (2.5 segundos)
            JPanel victoryPanel = new JPanel(new BorderLayout());
            victoryPanel.setBackground(new Color(53, 101, 77));

            // Imagen grande de victoria
            ImageIcon icon = new ImageIcon("src/VS/ganas.png");
            Image img = icon.getImage().getScaledInstance(800, 800, Image.SCALE_SMOOTH);
            JLabel victoryImage = new JLabel(new ImageIcon(img));
            victoryImage.setHorizontalAlignment(SwingConstants.CENTER);
            victoryPanel.add(victoryImage, BorderLayout.CENTER);

            // Nombres de los ganadores y suma victorias
            ArrayList<String> winners = new ArrayList<>();
            for (int i = 0; i < playersSums.size(); i++) {
                String resultado = evaluateWinner(playersSums.get(i));
                if (resultado.contains("Ganaste")) {
                    winners.add(playerNames.get(i));
                    victorias.put(playerNames.get(i), victorias.get(playerNames.get(i)) + 1);
                }
            }
            String winnerText;
            if (winners.size() == 1) {
                winnerText = winners.get(0) + " venció al dealer";
            } else {
                winnerText = String.join(", ", winners) + " vencieron al dealer";
            }

            JLabel victoryText = new JLabel(winnerText, SwingConstants.CENTER);
            victoryText.setFont(new Font("Arial", Font.BOLD, 40));
            victoryText.setForeground(Color.WHITE);
            victoryPanel.add(victoryText, BorderLayout.NORTH);

            JLabel abajo = new JLabel("¡Felicidades!", SwingConstants.CENTER);
            abajo.setFont(new Font("Arial", Font.BOLD, 28));
            abajo.setForeground(Color.YELLOW);
            victoryPanel.add(abajo, BorderLayout.SOUTH);

            // Tabla de victorias
            String[] columnas = {"Jugador", "Victorias"};
            String[][] datos = new String[playerNames.size()][2];
            for (int i = 0; i < playerNames.size(); i++) {
                datos[i][0] = playerNames.get(i);
                datos[i][1] = victorias.get(playerNames.get(i)).toString();
            }
            JTable tabla = new JTable(datos, columnas);
            tabla.setEnabled(false);
            tabla.setFont(new Font("Arial", Font.BOLD, 18));
            tabla.setRowHeight(28);
            tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(220, 180));
            victoryPanel.add(scroll, BorderLayout.EAST);

            // Botones
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(53, 101, 77));
            JButton playAgain = new JButton("Otra Partida");
            JButton mainMenu = new JButton("Menú Principal");
            playAgain.setFont(new Font("Arial", Font.BOLD, 22));
            mainMenu.setFont(new Font("Arial", Font.BOLD, 22));
            playAgain.setFocusable(false);
            mainMenu.setFocusable(false);
            buttonPanel.add(playAgain);
            buttonPanel.add(mainMenu);
            victoryPanel.add(buttonPanel, BorderLayout.PAGE_END);

            playAgain.addActionListener(ev -> {
                currentPlayer = 0;
                startGame();
                renderGameScreen();
            });

            mainMenu.addActionListener(ev -> {
                setupMenu();
            });

            frame.setContentPane(victoryPanel);
            frame.revalidate();
            frame.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    // Muestra la pantalla de derrota
    private void showLoseScreen() {
        Timer timer = new Timer(2500, e -> { // <-- Cambiado a 2500 ms (2.5 segundos)
            JPanel losePanel = new JPanel(new BorderLayout());
            losePanel.setBackground(new Color(53, 101, 77));

            // Imagen grande de derrota
            ImageIcon icon = new ImageIcon("src/VS/pierdes.png");
            Image img = icon.getImage().getScaledInstance(800, 800, Image.SCALE_SMOOTH);
            JLabel loseImage = new JLabel(new ImageIcon(img));
            loseImage.setHorizontalAlignment(SwingConstants.CENTER);
            losePanel.add(loseImage, BorderLayout.CENTER);

            // Texto grande arriba
            JLabel loseText = new JLabel("El dealer los derroto a TODOS", SwingConstants.CENTER);
            loseText.setFont(new Font("Arial", Font.BOLD, 40));
            loseText.setForeground(Color.WHITE);
            losePanel.add(loseText, BorderLayout.NORTH);

            // Texto grande abajo
            JLabel abajo = new JLabel("¡Inténtalo de nuevo!", SwingConstants.CENTER);
            abajo.setFont(new Font("Arial", Font.BOLD, 28));
            abajo.setForeground(Color.YELLOW);
            losePanel.add(abajo, BorderLayout.SOUTH);

            // Crear la tabla de victorias
            String[] columnas = {"Jugador", "Victorias"};
            String[][] datos = new String[playerNames.size()][2];
            for (int i = 0; i < playerNames.size(); i++) {
                datos[i][0] = playerNames.get(i);
                datos[i][1] = victorias.get(playerNames.get(i)).toString();
            }
            JTable tabla = new JTable(datos, columnas);
            tabla.setEnabled(false);
            tabla.setFont(new Font("Arial", Font.BOLD, 18));
            tabla.setRowHeight(28);
            tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
            JScrollPane scroll = new JScrollPane(tabla);
            scroll.setPreferredSize(new Dimension(220, 180));

            // Agrega la tabla a la derecha
            losePanel.add(scroll, BorderLayout.EAST);

            // Botones para jugar de nuevo o volver al menú
            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(53, 101, 77));
            JButton playAgain = new JButton("Otra Partida");
            JButton mainMenu = new JButton("Menú Principal");
            playAgain.setFont(new Font("Arial", Font.BOLD, 22));
            mainMenu.setFont(new Font("Arial", Font.BOLD, 22));
            playAgain.setFocusable(false);
            mainMenu.setFocusable(false);
            buttonPanel.add(playAgain);
            buttonPanel.add(mainMenu);
            losePanel.add(buttonPanel, BorderLayout.PAGE_END);

            playAgain.addActionListener(ev -> {
                currentPlayer = 0;
                startGame();
                renderGameScreen();
            });

            mainMenu.addActionListener(ev -> {
                setupMenu();
            });

            frame.setContentPane(losePanel);
            frame.revalidate();
            frame.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    // Método principal para iniciar el juego
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BlackJack());
    }
}
