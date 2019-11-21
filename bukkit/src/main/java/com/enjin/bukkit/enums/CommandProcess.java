package com.enjin.bukkit.enums;

public enum CommandProcess {

    EXECUTE(MessageAction.SEND),
    TAB(MessageAction.OMIT);

    private MessageAction messageAction;

    CommandProcess(MessageAction messageAction) {
        this.messageAction = messageAction;
    }

    public MessageAction getMessageAction() {
        return messageAction;
    }
}
