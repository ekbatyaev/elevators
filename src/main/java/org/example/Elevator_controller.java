package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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