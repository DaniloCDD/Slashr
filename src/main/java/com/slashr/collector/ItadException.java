package com.slashr.collector;


/**
 * Falha ao falar com o ITAD. O status é o HTTP retornado, ou 0 em falha de rede ou JSON.
 */
public class ItadException extends RuntimeException {

    private final int status;

    public ItadException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
