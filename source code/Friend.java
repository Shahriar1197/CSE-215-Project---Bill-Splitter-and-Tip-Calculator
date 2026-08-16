/**
 * Represents one member of the group: their name and how much cash
 * they contributed toward the bill.
 */
public class Friend {

    private String name;
    private double cashGiven;

    public Friend(String name) {
        this.name = name;
        this.cashGiven = 0.0;
    }

    // ----- Getters / Setters -----
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCashGiven() {
        return cashGiven;
    }

    public void setCashGiven(double cashGiven) {
        if (cashGiven < 0) {
            throw new IllegalArgumentException("Cash given cannot be negative.");
        }
        this.cashGiven = cashGiven;
    }

    @Override
    public String toString() {
        return name; // so Friend objects display nicely in JComboBox / JTable
    }
}