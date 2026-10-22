package com.slashr.collector;

public class ItadException extends RuntimeException {

    private final int status;

    public ItadException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int status() {
        return status;
    }

    //erro 429 é que excedeu o limite do ITAD
    public boolean limiteExcedido() {
        return status == 429;
    }
}
