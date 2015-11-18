package it.reactive.muskel.internal;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionTopicMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Boolean cancel;

    private final Long requestValues;
}
