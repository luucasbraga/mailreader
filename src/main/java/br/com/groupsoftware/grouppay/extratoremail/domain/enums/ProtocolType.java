package br.com.groupsoftware.grouppay.extratoremail.domain.enums;

public enum ProtocolType {

    IMAP("imap"), POP("pop3");

    private final String nome;

    ProtocolType(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

}
