package org.example;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Elevator_gui().Gui_show());
    }
}

class Elevator_gui {
    private static final int TOTAL_FLOORS = 10;
    private static final int TOTAL_ELEVATORS = 3;
    private final Elevator_controller controller;
    private final JLabel[][] elevator_indicators;

    public Elevator_gui() {
        controller = new Elevator_controller(TOTAL_ELEVATORS, TOTAL_FLOORS);
        elevator_indicators = new JLabel[TOTAL_FLOORS][TOTAL_ELEVATORS];
    }

    public void Gui_show() {
        JFrame frame = new JFrame("Симулятор работы лифтов");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel panel_build = new JPanel(new GridLayout(TOTAL_FLOORS, 1));
        for (int i = TOTAL_FLOORS - 1; i >= 0; i--) {
            JPanel floor_panel = new JPanel(new BorderLayout());
            floor_panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            JLabel floor_label = new JLabel("Этаж №" + (i + 1), SwingConstants.CENTER);

            JPanel panel_ping = new JPanel(new GridLayout(1, TOTAL_ELEVATORS));
            for (int j = 0; j < TOTAL_ELEVATORS; j++) {
                elevator_indicators[i][j] = new JLabel();
                elevator_indicators[i][j].setOpaque(true);
                elevator_indicators[i][j].setBackground(Color.WHITE);
                panel_ping.add(elevator_indicators[i][j]);
            }

            floor_panel.add(floor_label, BorderLayout.WEST);
            floor_panel.add(panel_ping, BorderLayout.CENTER);
            panel_build.add(floor_panel);
        }

        frame.add(panel_build, BorderLayout.CENTER);

        JButton button_call = new JButton("Вызвать лифт");
        button_call.setFont(new Font("Arial", Font.BOLD, 16));
        button_call.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(null, "Введите начальный и конечный этаж через пробел (например, '2 5'):", "Вызов лифта", JOptionPane.QUESTION_MESSAGE);
            try {
                String[] tokens = input.split(" ");
                int start_floor = Integer.parseInt(tokens[0]) - 1;
                int dest_floor = Integer.parseInt(tokens[1]) - 1;

                if (start_floor >= 0 && start_floor < TOTAL_FLOORS && dest_floor >= 0 && dest_floor < TOTAL_FLOORS) {
                    controller.request_elevator(start_floor, dest_floor);
                } else {
                    JOptionPane.showMessageDialog(null, "Неправильные этажи", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Неправильный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        frame.add(button_call, BorderLayout.SOUTH);

        frame.setSize(400, 800);
        frame.setVisible(true);

        controller.elevator_updating_listener(this::update_elevator_indicators);
    }

    private void update_elevator_indicators(int elevator_id, int cur_floor, String state) {
        for (int i = 0; i < TOTAL_FLOORS; i++) {
            elevator_indicators[i][elevator_id].setBackground(Color.WHITE);
        }
        elevator_indicators[cur_floor][elevator_id].setBackground(state.equals("Стоит") ? Color.GREEN : Color.YELLOW);
    }
}

interface Elevator_update_listener {
    void on_update(int elevator_id, int cur_floor, String state);
}
