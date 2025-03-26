package gr.atc.t4m.organization_management.model;

public enum MaasRole {
    CONSUMER(1),
    PROVIDER(2);

    private final int value;
    MaasRole(int value) { this.value = value; }
    public int getValue() { return value; }
}