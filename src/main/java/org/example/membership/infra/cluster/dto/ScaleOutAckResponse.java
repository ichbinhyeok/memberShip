package org.example.membership.infra.cluster.dto;

public record ScaleOutAckResponse(
        boolean ack,
        String message
) {}
