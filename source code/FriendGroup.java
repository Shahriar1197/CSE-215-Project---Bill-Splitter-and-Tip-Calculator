/**
 * Manages up to MAX_FRIENDS Friends and up to MAX_ITEMS BillItems
 * (fixed arrays, no ArrayList), plus the one Friend designated to
 * receive the pooled money, and does all the bill-splitting /
 * settlement math.
 *
 * NOTE ON THE FIX: items are now owned by the GROUP, not by a single
 * Friend. That's the only way a SharedAppetizer can correctly belong
 * to several people at once (see BillItem for details). FriendGroup
 * asks each item "how much does friend X owe you?" and sums that up
 * per friend, which is what actually makes the split work.
 */
public class FriendGroup {

    private Friend[] friends;
    private int friendCount;
    private Friend receiver;

    private BillItem[] items;
    private int itemCount;

    /** Who pays the tip. Null means "split proportionally by everyone's own subtotal". */
    private Friend tipPayer;

    public static final int MAX_FRIENDS = 20;
    public static final int MAX_ITEMS = 40;
    public static final double EPSILON = 0.01;

    public FriendGroup() {
        this.friends = new Friend[MAX_FRIENDS];
        this.friendCount = 0;
        this.items = new BillItem[MAX_ITEMS];
        this.itemCount = 0;
    }

    // ----- Friends -----
    public void addFriend(Friend friend) throws FriendGroupFullException {
        if (friendCount >= MAX_FRIENDS) {
            throw new FriendGroupFullException(
                    "Group is full (max " + MAX_FRIENDS + " friends).");
        }
        //   this line is equivalent to:
        //   friends[friendCount] = friend;
        //   friendCount++;
        friends[friendCount++] =  friend;
    }

    public void setReceiver(Friend receiver) {
        this.receiver = receiver;
    }

    public Friend getReceiver() {
        return receiver;
    }

    public Friend[] getFriends() {
        Friend[] result = new Friend[friendCount];
        System.arraycopy(friends, 0, result, 0, friendCount);
        return result;
    }

    public int getFriendCount() {
        return friendCount;
    }

    // ----- Items -----
    public void addItem(BillItem item) throws FriendGroupFullException {
        if (itemCount >= MAX_ITEMS) {
            throw new FriendGroupFullException(
                    "Item list is full (max " + MAX_ITEMS + " items).");
        }
        items[itemCount++] = item;
    }

    public BillItem[] getItems() {
        BillItem[] result = new BillItem[itemCount];
        System.arraycopy(items, 0, result, 0, itemCount);
        return result;
    }

    /** Every item a given friend is involved in, individual or shared. */
    public BillItem[] getItemsInvolving(Friend friend) {
        BillItem[] buffer = new BillItem[itemCount];
        int count = 0;
        for (int i = 0; i < itemCount; i++) {
            if (items[i].involves(friend)) {
                buffer[count++] = items[i];
            }
        }
        BillItem[] result = new BillItem[count];
        System.arraycopy(buffer, 0, result, 0, count);
        return result;
    }

    // ----- Tip -----
    public void setTipPayer(Friend tipPayer) {
        this.tipPayer = tipPayer;
    }

    public Friend getTipPayer() {
        return tipPayer;
    }

    // ----- Core money math -----

    /** Sum of every item's price BEFORE tax. */
    public double calculateSubtotal() {
        double total = 0.0;
        for (int i = 0; i < itemCount; i++) {
            total = total+ items[i].getPrice();
        }
        return total;
    }

    /** Sum of every item's price AFTER tax (each item counted exactly once). */
    public double calculateSubtotalWithTax() {
        double total = 0.0;
        for (int i = 0; i < itemCount; i++) {
            total = total + items[i].applyTax();
        }
        return total;
    }

    /** Just the tax portion of the whole bill. */
    public double calculateTotalTax() {
        return calculateSubtotalWithTax() - calculateSubtotal();
    }

    public double calculateTipAmount(double tipRate) {
        return calculateSubtotalWithTax() * tipRate;
    }

    public double calculateGrandTotal(double tipRate) {
        return calculateSubtotalWithTax() + calculateTipAmount(tipRate);
    }

    /** This friend's own food cost (taxed), individual items + their share of shared items. */
    public double calculateFriendSubtotal(Friend friend) {
        double total = 0.0;
        for (int i = 0; i < itemCount; i++) {
            total += items[i].getShareFor(friend);
        }
        return total;
    }

    /** This friend's slice of the tip -- either the full tip (if they're the designated payer) or a proportional share. */
    public double calculateFriendTipShare(Friend friend, double tipRate) {
        double tipAmount = calculateTipAmount(tipRate);
        if (tipAmount <= 0) {
            return 0.0;
        }
        if (tipPayer != null) {
            return friend.equals(tipPayer) ? tipAmount : 0.0;
        }
        double subtotalWithTax = calculateSubtotalWithTax();
        if (subtotalWithTax <= 0) {
            return 0.0;
        }
        double friendSubtotal = calculateFriendSubtotal(friend);
        return tipAmount * (friendSubtotal / subtotalWithTax);
    }

    /** Total amount this friend actually owes: their food (taxed) + their tip share. */
    public double calculateFriendFinalAmount(Friend friend, double tipRate) {
        return calculateFriendSubtotal(friend) + calculateFriendTipShare(friend, tipRate);
    }

    public double calculateTotalCollected() {
        double total = 0.0;
        for (int i = 0; i < friendCount; i++) {
            total += friends[i].getCashGiven();
        }
        return total;
    }

    /**
     * Positive = friend overpaid and should get that much back.
     * Negative = friend still owes that much more.
     */
    public double calculateChange(Friend friend, double tipRate) {
        return friend.getCashGiven() - calculateFriendFinalAmount(friend, tipRate);
    }

    // ----- Settlement -----

    /** One leg of the final settlement: who pays / receives how much. */
    public static class Transaction {
        private final Friend from; // null = paid out of the leftover cash pool ("restaurant change")
        private final Friend to;   // null = still owed back to the pool / receiver
        private final double amount;

        public Transaction(Friend from, Friend to, double amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public Friend getFrom() {
            return from;
        }

        public Friend getTo() {
            return to;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            if (from != null && to != null) {
                return String.format("%s pays %s Tk%.2f", from.getName(), to.getName(), amount);
            } else if (from == null && to != null) {
                return String.format("%s gets Tk%.2f back from the leftover cash", to.getName(), amount);
            } else if (from != null) {
                return String.format("%s still owes Tk%.2f to the group", from.getName(), amount);
            }
            return String.format("Tk%.2f unaccounted for", amount);
        }
    }

    /**
     * Corrected settlement using greedy matching between debtors and creditors,
     * including the group (null) as a participant.
     * All operations are performed with plain arrays (no ArrayList).
     */
    public Transaction[] computeSettlement(double tipRate) {
        Friend[] fs = getFriends();
        int n = fs.length;

        // Compute individual balances
        double[] balance = new double[n];
        for (int i = 0; i < n; i++) {
            balance[i] = calculateChange(fs[i], tipRate);
        }

        // Include the group's balance: positive = group is owed, negative = group owes
        double totalCollected = calculateTotalCollected();
        double grandTotal = calculateGrandTotal(tipRate);
        double groupBalance = grandTotal - totalCollected;

        // Maximum participants: n friends + 1 group
        int maxParticipants = n + 1;

        // Arrays to hold debtors and creditors (each as a Participant object)
        Participant[] debtors = new Participant[maxParticipants];
        Participant[] creditors = new Participant[maxParticipants];
        int debtorCount = 0;
        int creditorCount = 0;

        // Add friends
        for (int i = 0; i < n; i++) {
            if (balance[i] > EPSILON) {
                creditors[creditorCount++] = new Participant(fs[i], balance[i]);
            } else if (balance[i] < -EPSILON) {
                debtors[debtorCount++] = new Participant(fs[i], balance[i]);
            }
        }

        // Add group (represented by null friend)
        if (Math.abs(groupBalance) > EPSILON) {
            if (groupBalance > 0) {
                // group is a creditor (someone owes the group)
                creditors[creditorCount++] = new Participant(null, groupBalance);
            } else {
                // group is a debtor (group owes money back)
                debtors[debtorCount++] = new Participant(null, groupBalance);
            }
        }

        // Prepare transaction buffer (max possible transactions: debtors * creditors)
        Transaction[] buffer = new Transaction[maxParticipants * maxParticipants];
        int transactionCount = 0;

        // Greedy settlement: match first debtor with first creditor
        int dIdx = 0, cIdx = 0;
        while (dIdx < debtorCount && cIdx < creditorCount) {
            Participant debtor = debtors[dIdx];
            Participant creditor = creditors[cIdx];
            double debt = -debtor.balance;   // debtor.balance is negative
            double credit = creditor.balance; // creditor.balance is positive
            double amount = Math.min(debt, credit);
            if (amount > EPSILON) {
                buffer[transactionCount++] = new Transaction(debtor.friend, creditor.friend, amount);
                debtor.balance += amount;   // becomes less negative
                creditor.balance -= amount; // becomes less positive
            }
            // Remove participants with zero balance (within epsilon)
            if (debtor.balance >= -EPSILON) {
                dIdx++;
            }
            if (creditor.balance <= EPSILON) {
                cIdx++;
            }
        }

        // Copy result into array of correct length
        Transaction[] result = new Transaction[transactionCount];
        System.arraycopy(buffer, 0, result, 0, transactionCount);
        return result;
    }

    // Helper class to hold a participant (friend or group) and its balance
    private static class Participant {
        Friend friend; // null for the group
        double balance;

        Participant(Friend friend, double balance) {
            this.friend = friend;
            this.balance = balance;
        }
    }
}