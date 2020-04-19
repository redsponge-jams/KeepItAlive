package com.redsponge.keepitalive;

public enum DeathCause {
    HOST_DIED("Your Host Died!"),
    HOST_HEALED("Your Host Got Healed!")

    ;

    private final String msg;
    DeathCause(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
