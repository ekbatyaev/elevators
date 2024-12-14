package org.example;

public class Request {
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
