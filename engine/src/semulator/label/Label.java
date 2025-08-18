package semulator.label;

public interface Label {
    String getLabel(); // label name like "L1" or "LOOP"

    int getAddress(); // instruction the label is pointing at

    String getLabelRepresentation();

    default boolean isExit() {
        return false;
    }

}
