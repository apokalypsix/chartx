package com.apokalypsix.chartx.core.ui.properties.editors;

import com.apokalypsix.chartx.core.ui.properties.PropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Property editor for Color values using a color swatch with picker popup.
 *
 * <p>Displays a colored rectangle that opens a color chooser dialog when clicked.
 */
public class ColorPropertyEditor implements PropertyEditor<Color> {

    // Dark theme colors
    private static final Color BACKGROUND = new Color(45, 47, 51);
    private static final Color BORDER_COLOR = new Color(60, 62, 66);
    private static final Color BORDER_HOVER = new Color(100, 102, 106);

    private final JPanel panel;
    private final ColorSwatch swatch;
    private Color value;
    private Consumer<Color> onValueChanged;

    /**
     * Creates a color editor with the specified initial color.
     *
     * @param initialColor the initial color
     */
    public ColorPropertyEditor(Color initialColor) {
        this.value = initialColor != null ? initialColor : Color.WHITE;

        panel = new JPanel(new BorderLayout(5, 0));
        panel.setBackground(BACKGROUND);
        panel.setPreferredSize(new Dimension(100, 24));

        swatch = new ColorSwatch();
        swatch.setColor(value);
        panel.add(swatch, BorderLayout.WEST);

        // Hex label
        JLabel hexLabel = new JLabel(colorToHex(value));
        hexLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        hexLabel.setForeground(new Color(180, 182, 186));
        panel.add(hexLabel, BorderLayout.CENTER);

        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showColorChooser();
            }
        });

        // Update hex label when color changes
        swatch.addPropertyChangeListener("color", evt -> {
            hexLabel.setText(colorToHex((Color) evt.getNewValue()));
        });
    }

    private void showColorChooser() {
        Window owner = SwingUtilities.getWindowAncestor(panel);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;

        // Pass callback for live updates
        SimpleColorDialog dialog = new SimpleColorDialog(frame, value, newColor -> {
            value = newColor;
            swatch.setColor(value);
            if (onValueChanged != null) {
                onValueChanged.accept(value);
            }
        });
        dialog.setVisible(true);
    }

    /**
     * Color picker dialog with a color wheel, brightness slider, and alpha.
     * Changes are applied dynamically as selections change.
     */
    private static class SimpleColorDialog extends JDialog {
        private static final Color DIALOG_BG = new Color(30, 32, 36);
        private static final Color PANEL_BG = new Color(40, 42, 46);
        private static final Color TEXT_COLOR = new Color(220, 222, 226);
        private static final Color LABEL_COLOR = new Color(180, 182, 186);
        private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        private final ColorWheel colorWheel;
        private final JSlider brightnessSlider;
        private final JSlider alphaSlider;
        private final ColorPreview preview;
        private final Consumer<Color> onColorChanged;
        private int alpha;

        SimpleColorDialog(Frame owner, Color initialColor, Consumer<Color> onColorChanged) {
            super(owner, "Choose Color", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.onColorChanged = onColorChanged;

            Color color = initialColor != null ? initialColor : Color.WHITE;
            this.alpha = color.getAlpha();

            // Convert to HSB
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

            // Main panel
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBackground(DIALOG_BG);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // Center panel with wheel and preview
            JPanel centerPanel = new JPanel(new BorderLayout(15, 10));
            centerPanel.setBackground(DIALOG_BG);

            // Color wheel
            colorWheel = new ColorWheel(150, hsb[0], hsb[1], hsb[2]);
            colorWheel.setOnColorChanged(this::updateFromWheel);
            JPanel wheelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            wheelPanel.setBackground(DIALOG_BG);
            wheelPanel.add(colorWheel);
            centerPanel.add(wheelPanel, BorderLayout.CENTER);

            // Preview on the right
            preview = new ColorPreview(color);
            preview.setPreferredSize(new Dimension(50, 50));
            JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            previewPanel.setBackground(DIALOG_BG);
            previewPanel.add(preview);
            centerPanel.add(previewPanel, BorderLayout.EAST);

            mainPanel.add(centerPanel, BorderLayout.CENTER);

            // Sliders panel
            JPanel slidersPanel = new JPanel(new GridBagLayout());
            slidersPanel.setBackground(PANEL_BG);
            slidersPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 62, 66)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;

            // Brightness slider
            brightnessSlider = createSlider((int) (hsb[2] * 100));
            brightnessSlider.setMaximum(100);
            addSliderRow(slidersPanel, gbc, 0, "Brightness", brightnessSlider);
            brightnessSlider.addChangeListener(e -> {
                colorWheel.setBrightness(brightnessSlider.getValue() / 100f);
                updatePreview();
            });

            // Alpha slider
            alphaSlider = createSlider(color.getAlpha());
            alphaSlider.setMaximum(255);
            addSliderRow(slidersPanel, gbc, 1, "Alpha", alphaSlider);
            alphaSlider.addChangeListener(e -> {
                alpha = alphaSlider.getValue();
                updatePreview();
            });

            mainPanel.add(slidersPanel, BorderLayout.SOUTH);

            // Close button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(PANEL_BG);

            JButton closeButton = createButton("Close", true);
            closeButton.addActionListener(e -> dispose());
            buttonPanel.add(closeButton);

            // Add button panel to sliders panel
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(10, 5, 0, 5);
            slidersPanel.add(buttonPanel, gbc);

            setContentPane(mainPanel);
            pack();
            setMinimumSize(new Dimension(280, 320));
            setLocationRelativeTo(owner);

            // Escape key closes
            getRootPane().registerKeyboardAction(
                    e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            getRootPane().setDefaultButton(closeButton);
        }

        private void updateFromWheel() {
            updatePreview();
        }

        private JSlider createSlider(int value) {
            JSlider slider = new JSlider(0, 255, value);
            slider.setBackground(PANEL_BG);
            slider.setForeground(TEXT_COLOR);
            slider.setPreferredSize(new Dimension(150, 20));
            return slider;
        }

        private void addSliderRow(JPanel panel, GridBagConstraints gbc, int row, String label, JSlider slider) {
            gbc.gridy = row;

            // Label
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel lbl = new JLabel(label);
            lbl.setFont(LABEL_FONT);
            lbl.setForeground(LABEL_COLOR);
            lbl.setPreferredSize(new Dimension(70, 20));
            panel.add(lbl, gbc);

            // Slider
            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(slider, gbc);
        }

        private void updatePreview() {
            Color newColor = getSelectedColor();
            preview.setColor(newColor);
            if (onColorChanged != null) {
                onColorChanged.accept(newColor);
            }
        }

        private JButton createButton(String text, boolean primary) {
            JButton button = new JButton(text);
            button.setFont(LABEL_FONT);
            button.setForeground(TEXT_COLOR);
            button.setBackground(primary ? new Color(38, 166, 154) : new Color(50, 52, 56));
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(80, 28));
            return button;
        }

        private Color getSelectedColor() {
            Color rgb = colorWheel.getSelectedColor();
            return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
        }
    }

    /**
     * Color wheel component for selecting hue and saturation.
     */
    private static class ColorWheel extends JPanel {
        private static final int SELECTOR_RADIUS = 6;

        private final int size;
        private float hue;
        private float saturation;
        private float brightness;
        private Image wheelImage;
        private Runnable onColorChanged;

        ColorWheel(int size, float hue, float saturation, float brightness) {
            this.size = size;
            this.hue = hue;
            this.saturation = saturation;
            this.brightness = brightness;

            setPreferredSize(new Dimension(size, size));
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    updateFromMouse(e.getX(), e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    updateFromMouse(e.getX(), e.getY());
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        void setOnColorChanged(Runnable callback) {
            this.onColorChanged = callback;
        }

        void setBrightness(float brightness) {
            this.brightness = brightness;
            wheelImage = null; // Regenerate wheel
            repaint();
        }

        Color getSelectedColor() {
            return Color.getHSBColor(hue, saturation, brightness);
        }

        private void updateFromMouse(int x, int y) {
            int centerX = size / 2;
            int centerY = size / 2;
            int radius = size / 2 - 2;

            double dx = x - centerX;
            double dy = y - centerY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Clamp to circle
            if (dist > radius) {
                dx = dx * radius / dist;
                dy = dy * radius / dist;
                dist = radius;
            }

            // Calculate hue from angle
            hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI));

            // Calculate saturation from distance
            saturation = (float) (dist / radius);

            repaint();
            if (onColorChanged != null) {
                onColorChanged.run();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int centerX = size / 2;
            int centerY = size / 2;
            int radius = size / 2 - 2;

            // Generate wheel image if needed
            if (wheelImage == null) {
                wheelImage = createWheelImage(size, radius, brightness);
            }
            g2.drawImage(wheelImage, 0, 0, null);

            // Draw border
            g2.setColor(new Color(60, 62, 66));
            g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            // Draw selector
            double angle = hue * 2 * Math.PI - Math.PI;
            int selectorX = centerX + (int) (Math.cos(angle) * saturation * radius);
            int selectorY = centerY + (int) (Math.sin(angle) * saturation * radius);

            // Outer ring (dark)
            g2.setColor(Color.BLACK);
            g2.fillOval(selectorX - SELECTOR_RADIUS, selectorY - SELECTOR_RADIUS,
                    SELECTOR_RADIUS * 2, SELECTOR_RADIUS * 2);
            // Inner ring (white)
            g2.setColor(Color.WHITE);
            g2.fillOval(selectorX - SELECTOR_RADIUS + 2, selectorY - SELECTOR_RADIUS + 2,
                    SELECTOR_RADIUS * 2 - 4, SELECTOR_RADIUS * 2 - 4);
            // Center with selected color
            g2.setColor(getSelectedColor());
            g2.fillOval(selectorX - SELECTOR_RADIUS + 4, selectorY - SELECTOR_RADIUS + 4,
                    SELECTOR_RADIUS * 2 - 8, SELECTOR_RADIUS * 2 - 8);

            g2.dispose();
        }

        private Image createWheelImage(int size, int radius, float brightness) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);

            int centerX = size / 2;
            int centerY = size / 2;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    if (dist <= radius) {
                        float h = (float) ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI));
                        float s = (float) (dist / radius);
                        int rgb = Color.HSBtoRGB(h, s, brightness);
                        img.setRGB(x, y, rgb);
                    } else {
                        img.setRGB(x, y, 0); // Transparent
                    }
                }
            }
            return img;
        }
    }

    /**
     * Color preview panel showing the selected color.
     */
    private static class ColorPreview extends JPanel {
        private Color color;

        ColorPreview(Color color) {
            this.color = color;
            setBorder(BorderFactory.createLineBorder(new Color(60, 62, 66)));
        }

        void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            int w = getWidth();
            int h = getHeight();

            // Checkerboard for transparency
            int checkSize = 8;
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.LIGHT_GRAY);
            for (int y = 0; y < h; y += checkSize) {
                for (int x = 0; x < w; x += checkSize) {
                    if ((x / checkSize + y / checkSize) % 2 == 0) {
                        g2.fillRect(x, y, checkSize, checkSize);
                    }
                }
            }

            // Draw color
            g2.setColor(color);
            g2.fillRect(0, 0, w, h);

            g2.dispose();
        }
    }

    private String colorToHex(Color c) {
        if (c.getAlpha() < 255) {
            return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public Color getValue() {
        return value;
    }

    @Override
    public void setValue(Color value) {
        this.value = value != null ? value : Color.WHITE;
        swatch.setColor(this.value);
    }

    @Override
    public void setOnValueChanged(Consumer<Color> callback) {
        this.onValueChanged = callback;
    }

    /**
     * Inner class for the color swatch display.
     */
    private static class ColorSwatch extends JPanel {
        private Color color = Color.WHITE;
        private boolean hovered = false;

        ColorSwatch() {
            setPreferredSize(new Dimension(24, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setColor(Color color) {
            Color oldColor = this.color;
            this.color = color;
            firePropertyChange("color", oldColor, color);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int inset = 2;

            // Draw checkerboard pattern for transparency
            int checkSize = 4;
            g2.setColor(Color.WHITE);
            g2.fillRect(inset, inset, w - inset * 2, h - inset * 2);
            g2.setColor(Color.LIGHT_GRAY);
            for (int y = inset; y < h - inset; y += checkSize) {
                for (int x = inset; x < w - inset; x += checkSize) {
                    if (((x - inset) / checkSize + (y - inset) / checkSize) % 2 == 0) {
                        g2.fillRect(x, y,
                                Math.min(checkSize, w - inset - x),
                                Math.min(checkSize, h - inset - y));
                    }
                }
            }

            // Draw color swatch
            g2.setColor(color);
            g2.fillRect(inset, inset, w - inset * 2, h - inset * 2);

            // Draw border
            g2.setColor(hovered ? BORDER_HOVER : BORDER_COLOR);
            g2.drawRect(inset, inset, w - inset * 2 - 1, h - inset * 2 - 1);

            g2.dispose();
        }
    }
}
