package it.reactive.muskel.context;

public interface MuskelMessageListener<T> {

    void onMessage(Message<T> message);
}
