package it.reactive.muskel.context;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    protected T messageObject;

}
