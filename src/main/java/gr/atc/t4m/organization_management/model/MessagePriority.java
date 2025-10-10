package gr.atc.t4m.organization_management.model;

/*
 * Enum for Message Priority
 */
public enum MessagePriority {
    Low("Low"),
    Mid("Mid"),
    High("High");

    private final String priority;

    MessagePriority(final String priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return priority;
    }

}

