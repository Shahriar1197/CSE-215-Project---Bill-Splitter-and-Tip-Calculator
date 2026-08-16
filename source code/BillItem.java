/**
 * Abstract base class for anything that appears on the bill.
 * Demonstrates ABSTRACTION (abstract methods below) and
 * ENCAPSULATION (private fields with getters/setters).
 */
public abstract class BillItem implements Taxable {

    private String itemName;
    private double price;

    private static final double TAX_RATE = 0.05; // 5% flat tax

    public BillItem() {
        this("Unnamed Item", 0.0);
    }

    public BillItem(String itemName, double price) {
        this.itemName = itemName;
        setPrice(price);
    }

    // Getters / Setters
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.price = price;
    }

    public static double getTaxRate() {
        return TAX_RATE;
    }

    // ----- Taxable implementation -----
    @Override
    public double applyTax() {
        return price + (price * TAX_RATE);
    }

    /** Just the tax portion of this item's price. */
    public double getTaxAmount() {
        return price * TAX_RATE;
    }

    //Polymorphic behavior, implemented differently by each subclass

    /** True if the given friend is one of the people who has to pay for this item. */
    public abstract boolean involves(Friend friend);

    /** How many people this item's (taxed) price is split across. */
    public abstract int getParticipantCount();

    /**
     * The amount of this item's TAXED price that the given friend is
     * responsible for. Returns 0.0 if the friend has nothing to do with
     * this it
     */
    public abstract double getShareFor(Friend friend);

    /**List of who this item belongs to / is shared by. */
    public abstract String getParticipantsDescription();

    @Override
    public String toString() {
        return String.format("%s (Tk%.2f)", itemName, price);
    }
}