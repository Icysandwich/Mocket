package mocket.path;

import java.util.HashSet;

public interface Action {
    ActionType getType();
    HashSet<String> getParameters();
    boolean compare(Action a);
    boolean isSingleNodeAction();
    boolean isMessageRelatedAction();
    boolean isClientRequest();
    boolean isExternalFault();

}
