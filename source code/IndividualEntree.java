/**
 * A dish ordered by a single person; the full taxed price belongs
 * to whichever Friend ordered it.
 */
public class IndividualEntree extends BillItem {

    private Friend owner;

    public IndividualEntree() {
        super();
    }

    public IndividualEntree(String itemName, double price, Friend owner) {
        super(itemName, price);
        if (owner == null) {
            throw new IllegalArgumentException("An individual entree must have an owner.");
        }
        this.owner = owner;
    }

    public Friend getOwner() {
        return owner;
    }

    public void setOwner(Friend owner) {
        this.owner = owner;
    }

    @Override
    public boolean involves(Friend friend) {
        return owner != null && owner.equals(friend);
    }

    @Override
    public int getParticipantCount() {
        return 1;
    }

    @Override
    public double getShareFor(Friend friend) {
        // Not shared -- the full taxed price is charged to the owner only.
        return involves(friend) ? applyTax() : 0.0;
    }

    @Override
    public String getParticipantsDescription() {
        return owner != null ? owner.getName() : "(no owner)";
    }
}