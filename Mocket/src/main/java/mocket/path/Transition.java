package mocket.path;

public class Transition {
    public Transition next;
    public Transition prev;
    public int sid;
    public Action action; // The action that results in this state
    public State state;
    private boolean aChecked = false;
    private boolean sChecked = false;
//    String ID;

    public Transition(Transition prev, int sid, Action action, State state){
        this.state = state;
        this.sid = sid;
        this.action = action;
        if(prev != null) {
            this.prev = prev;
            prev.next = this;
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public State getState() {
        return this.state;
    }

    public boolean isInitialState() {
        return action.getActionType().equals(ActionType.NULL);
    }

    public boolean isActionExecuted() {
        return this.aChecked;
    }

    public void executed() {
        this.aChecked = true;
    }

    // Only for inital state checking
    public void checked() {
        if (!aChecked)
            this.aChecked = true; 
        this.sChecked = true;
    }

    public boolean isStateChecked() {
        if (!aChecked)
            return false;
        // We do not check states after client requests and injected faults
        else if (action.isClientRequest() || action.isExternalFault())
            return aChecked;
        else
            return sChecked;
    }

    @Override
    public String toString(){
        if(isInitialState()) {
            return "Initial state";
        } else {
            return this.action.getActionType().getType() + " at Node " + action.getSid();
        }
    }
}