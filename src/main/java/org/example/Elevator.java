package org.example;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Elevator implements Runnable {
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