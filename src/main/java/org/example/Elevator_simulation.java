package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Elevator_simulation {
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

class Elevator_controller {
    private final List<Elevator> elevators;
    private final ExecutorService executor;

    public Elevator_controller(int num_elevators, int total_floors) {
        this.elevators = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(num_elevators);
        for (int i = 0; i < num_elevators; i++) {
            Elevator elevator = new Elevator(i, total_floors);
            elevators.add(elevator);
            executor.submit(elevator);
        }
    }

    public void elevator_updating_listener(Elevator_update_listener listener) {
        elevators.forEach(elevator -> elevator.set_update_listener(listener));
    }

    public void request_elevator(int start_floor, int dest_floor) {
        synchronized (elevators) {
            Elevator best_elevator = null;
            int min_cost = Integer.MAX_VALUE;

            for (Elevator elevator : elevators) {
                int cost = elevator.calculate_cost(start_floor);
                if (cost < min_cost) {
                    min_cost = cost;
                    best_elevator = elevator;
                }
            }

            if (best_elevator != null) {
                best_elevator.add_stop(start_floor, dest_floor);
            }
        }
    }
}

class Elevator implements Runnable {
    private final int id;
    private final int total_floors;
    private final Queue<Request> requests;
    private final AtomicInteger current_floor;
    private Elevator_update_listener update_listener;
    private boolean moving_up;

    public Elevator(int id, int total_floors) {
        this.id = id;
        this.total_floors = total_floors;
        this.requests = new LinkedList<>();
        this.current_floor = new AtomicInteger(0);
        this.moving_up = true;
    }

    public int calculate_cost(int floor) {
        synchronized (requests) {
            int distance = Math.abs(current_floor.get() - floor);
            return distance + requests.size();
        }
    }

    public void add_stop(int start_floor, int dest_floor) {
        synchronized (requests) {
            requests.offer(new Request(start_floor, dest_floor));
            synchronized (this) {
                notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            Request next_request;

            synchronized (this) {
                while (requests.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                synchronized (requests) {
                    next_request = requests.poll();
                }
            }

            if (next_request != null) {
                move_to_floor(next_request.get_start_floor());
                move_to_floor(next_request.get_dest_floor());
            }
        }
    }

    private void move_to_floor(int floor) {
        try {
            while (current_floor.get() != floor) {
                if (current_floor.get() < floor) {
                    moving_up = true;
                    current_floor.incrementAndGet();
                } else {
                    moving_up = false;
                    current_floor.decrementAndGet();
                }
                update_state("Двигается");
                Thread.sleep(1000);
            }
            update_state("Стоит");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void update_state(String state) {
        if (update_listener != null) {
            update_listener.on_update(id, current_floor.get(), state);
        }
    }
    public void set_update_listener(Elevator_update_listener listener) {
        this.update_listener = listener;
    }
}

class Request {
    private final int start_floor;
    private final int dest_floor;

    public Request(int start_floor, int dest_floor) {
        this.start_floor = start_floor;
        this.dest_floor = dest_floor;
    }

    public int get_start_floor() {
        return start_floor;
    }

    public int get_dest_floor() {
        return dest_floor;
    }
}

interface Elevator_update_listener {
    void on_update(int elevator_id, int cur_floor, String state);
}
