package com.github.hichemtabtech.jettreemark.toolwindow;

import javax.swing.*;
import java.awt.*;

/**
 * A checkbox that can be in one of three states: selected, unselected, or indeterminate.
 */
public class TriStateCheckBox extends JCheckBox {
    public enum State { SELECTED, UNSELECTED, INDETERMINATE }

    private State state = State.UNSELECTED;

    public TriStateCheckBox() {
        super();
        setState(State.UNSELECTED);
    }

    @SuppressWarnings("unused")
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;

        // Update the visual appearance
        switch (state) {
            case SELECTED:
                setSelected(true);
                setIcon(null); // Use the default selected icon
                break;
            case UNSELECTED:
                setSelected(false);
                setIcon(null); // Use default unselected icon
                break;
            case INDETERMINATE:
                setSelected(false);
                // Create a custom icon for an indeterminate state (a dash in the checkbox)
                setIcon(createIndeterminateIcon());
                break;
        }
    }

    private Icon createIndeterminateIcon() {
        // Create a custom icon that shows a dash in the checkbox
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                // Get the default checkbox icon
                Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                if (defaultIcon != null) {
                    // Draw the default unchecked icon
                    defaultIcon.paintIcon(c, g, x, y);

                    // Draw a dash in the middle
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(getForeground());
                    int width = getIconWidth();
                    int height = getIconHeight();
                    int dashWidth = width / 2;
                    int dashHeight = height / 6;
                    g2d.fillRect(x + (width - dashWidth) / 2, y + (height - dashHeight) / 2, dashWidth, dashHeight);
                    g2d.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                return defaultIcon != null ? defaultIcon.getIconWidth() : 16;
            }

            @Override
            public int getIconHeight() {
                Icon defaultIcon = UIManager.getIcon("CheckBox.icon");
                return defaultIcon != null ? defaultIcon.getIconHeight() : 16;
            }
        };
    }
}