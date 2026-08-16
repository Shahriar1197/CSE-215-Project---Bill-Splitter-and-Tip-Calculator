/**
 * A dish shared by SOME subset of the group; its taxed cost is split
 * evenly across only the people who actually shared it (not necessarily
 * everyone in the FriendGroup).
 */
public class SharedAppetizer extends BillItem {

    private Friend[] sharers;
    private int sharerCount;

    public SharedAppetizer() {
        super();
        this.sharers = new Friend[0];
        this.sharerCount = 0;
    }

    public SharedAppetizer(String itemName, double price, Friend[] sharers) {
        super(itemName, price);
        if (sharers == null || sharers.length == 0) {
            throw new IllegalArgumentException("A shared appetizer needs at least one person sharing it.");
        }
        this.sharers = new Friend[sharers.length];
        for (int i = 0; i < sharers.length; i++) {
            this.sharers[i] = sharers[i];
        }
        this.sharerCount = sharers.length;
    }

    public Friend[] getSharers() {
        Friend[] result = new Friend[sharerCount];
        System.arraycopy(sharers, 0, result, 0, sharerCount);
        return result;
    }

    @Override
    public boolean involves(Friend friend) {
        if (friend == null) {
            return false;
        }
        for (int i = 0; i < sharerCount; i++) {
            if (sharers[i].equals(friend)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getParticipantCount() {
        return sharerCount;
    }

    @Override
    public double getShareFor(Friend friend) {
        if (sharerCount <= 0) {
            throw new IllegalArgumentException("Total sharers must be greater than zero.");
        }
        // Split the TAXED price only across the people who actually shared it.
        return involves(friend) ? (applyTax() / sharerCount) : 0.0;
    }

    @Override
    public String getParticipantsDescription() {
        if (sharerCount == 0) {
            return "(no one)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sharerCount; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(sharers[i].getName());
        }
        return sb.toString();
    }
}