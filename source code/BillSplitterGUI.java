import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main Swing application.
 */
public class BillSplitterGUI extends JFrame {

    private static final String TIP_SPLIT_EVENLY = "Proportional to each person's bill";

    // ----- Our own class that stores all the real data -----
    private final FriendGroup friendGroup;

    // ----- Admin Panel components -----
    private JTextField friendNameField;
    private JTextField cashField;

    private JTextField itemNameField;
    private JTextField priceField;
    private JComboBox<String> itemTypeBox;

    private JLabel ownerLabel;
    private JComboBox<Friend> friendSelectBox;   // owner picker for Individual Entree

    private JLabel sharerLabel;
    private JCheckBox[] sharerCheckBoxes;        // one checkbox per possible friend slot

    private JComboBox<String> tipBox;
    private JComboBox<Object> tipPayerBox;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea txtOutput;

    // ----- User Panel components -----
    private JComboBox<Friend> userSelectBox;
    private JTextArea userOutput;

    public BillSplitterGUI() {
        super("Bill Splitter & Tip Calculator");
        friendGroup = new FriendGroup();
        sharerCheckBoxes = new JCheckBox[FriendGroup.MAX_FRIENDS];

        setSize(920, 770);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Admin Panel", buildAdminPanel());
        tabs.addTab("User Panel", buildUserPanel());
        add(tabs);
    }

    // =========================================================
    // ADMIN PANEL
    // =========================================================
    private JPanel buildAdminPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);                      // we place everything ourselves
        panel.setPreferredSize(new Dimension(900, 720));
        panel.setBackground(Color.WHITE);

        //  Row 1 : Add Friend
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setBounds(10, 15, 50, 25);
        panel.add(nameLabel);

        friendNameField = new JTextField();
        friendNameField.setBounds(65, 15, 140, 25);
        panel.add(friendNameField);

        JLabel cashLabel = new JLabel("Cash Given (Tk):");
        cashLabel.setBounds(215, 15, 100, 25);
        panel.add(cashLabel);

        cashField = new JTextField();
        cashField.setBounds(320, 15, 80, 25);
        panel.add(cashField);

        JButton btnAddFriend = new JButton("Add Friend");
        btnAddFriend.setBounds(410, 12, 130, 30);
        btnAddFriend.addActionListener(e -> addFriend());
        panel.add(btnAddFriend);

        // Row 2 : Add Item (name, price, type)
        JLabel itemLabel = new JLabel("Item:");
        itemLabel.setBounds(10, 55, 50, 25);
        panel.add(itemLabel);

        itemNameField = new JTextField();
        itemNameField.setBounds(65, 55, 140, 25);
        panel.add(itemNameField);

        JLabel priceLabel = new JLabel("Price (Tk):");
        priceLabel.setBounds(215, 55, 70, 25);
        panel.add(priceLabel);

        priceField = new JTextField();
        priceField.setBounds(290, 55, 80, 25);
        panel.add(priceField);

        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setBounds(380, 55, 45, 25);
        panel.add(typeLabel);

        itemTypeBox = new JComboBox<>(new String[]{"Individual Entree", "Shared Appetizer"});
        itemTypeBox.setBounds(425, 55, 190, 25);
        itemTypeBox.addActionListener(e -> updateOwnerArea());
        panel.add(itemTypeBox);

        // ----- Row 3 : Owner (Individual) OR Sharer checkboxes (Shared) -----
        // Both groups sit at the same spot on screen. We only show ONE of
        // them at a time using setVisible(...), instead of a CardLayout.
        ownerLabel = new JLabel("Owner:");
        ownerLabel.setBounds(10, 95, 60, 25);
        panel.add(ownerLabel);

        friendSelectBox = new JComboBox<>();
        friendSelectBox.setBounds(75, 95, 200, 25);
        panel.add(friendSelectBox);

        sharerLabel = new JLabel("Tick who shared this item:");
        sharerLabel.setBounds(10, 95, 180, 25);
        panel.add(sharerLabel);

        // Create MAX_FRIENDS checkboxes up front. Only the ones for
        // friends that actually exist are made visible (see
        // refreshFriendControls()). This keeps things simple: no
        // dynamic lists, just a fixed-size array, same idea as the
        // fixed-size arrays already used inside FriendGroup.
        int boxX = 195;
        for (int i = 0; i < sharerCheckBoxes.length; i++) {
            JCheckBox box = new JCheckBox("Friend " + (i + 1));
            box.setBounds(boxX, 95, 115, 25);
            box.setVisible(false);
            panel.add(box);
            sharerCheckBoxes[i] = box;
            boxX = boxX + 115;
        }

        JButton btnAddItem = new JButton("Add Item");
        btnAddItem.setBounds(10, 130, 150, 30);
        btnAddItem.addActionListener(e -> addBillItem());
        panel.add(btnAddItem);

        // ----- Row 5 : Tip + Calculate + Save/Load -----
        JLabel tipLabel = new JLabel("Tip %:");
        tipLabel.setBounds(10, 175, 50, 25);
        panel.add(tipLabel);

        tipBox = new JComboBox<>(new String[]{"10%", "15%", "18%", "20%"});
        tipBox.setBounds(65, 175, 90, 25);
        panel.add(tipBox);

        JLabel tipPayerLabel = new JLabel("Tip Paid By:");
        tipPayerLabel.setBounds(170, 175, 80, 25);
        panel.add(tipPayerLabel);

        tipPayerBox = new JComboBox<>();
        tipPayerBox.addItem(TIP_SPLIT_EVENLY);
        tipPayerBox.setBounds(255, 175, 220, 25);
        panel.add(tipPayerBox);

        JButton btnCalculate = new JButton("Calculate Final Bill");
        btnCalculate.setBounds(490, 172, 170, 30);
        btnCalculate.addActionListener(e -> calculateFinalBill());
        panel.add(btnCalculate);


        // ----- Table of friends -----
        String[] columns = {"Friend", "Cash Given (Tk)", "Items Involved In", "Food Subtotal w/ Tax (Tk)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only, just a display
            }
        };
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBounds(10, 215, 885, 160);
        panel.add(tableScroll);

        // ----- Final bill output -----
        txtOutput = new JTextArea();
        txtOutput.setEditable(false);
        txtOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(txtOutput);
        outputScroll.setBounds(10, 385, 885, 320);
        panel.add(outputScroll);

        updateOwnerArea();
        return panel;
    }

    /** Shows the owner combo box or the sharer checkboxes, depending on the item type picked. */
    private void updateOwnerArea() {
        String type = (String) itemTypeBox.getSelectedItem();
        boolean isShared = "Shared Appetizer".equals(type);

        ownerLabel.setVisible(!isShared);
        friendSelectBox.setVisible(!isShared);

        sharerLabel.setVisible(isShared);
        for (int i = 0; i < sharerCheckBoxes.length; i++) {
            // Only make a checkbox visible if BOTH the item type is
            // "Shared" AND a real friend actually exists in that slot.
            boolean friendExists = i < friendGroup.getFriendCount();
            sharerCheckBoxes[i].setVisible(isShared && friendExists);
        }
    }

    // ----- Admin actions -----

    /** Reads the Add Friend fields and adds a new Friend to the group. */
    private void addFriend() {
        try {
            String name = friendNameField.getText().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Friend name cannot be empty.");
            }
            double cash = Double.parseDouble(cashField.getText().trim());

            Friend friend = new Friend(name);
            friend.setCashGiven(cash);
            friendGroup.addFriend(friend); // may throw FriendGroupFullException

            if (friendGroup.getFriendCount() == 1) {
                friendGroup.setReceiver(friend); // first friend added is the default receiver
            }

            friendNameField.setText("");
            cashField.setText("");
            refreshFriendControls();
            refreshTable();

        } catch (NumberFormatException ex) {
            showError("Cash given must be a valid number.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (FriendGroupFullException ex) {
            showError(ex.getMessage());
        }
    }

    /** Reads the Add Item fields and adds a BillItem to the group. */
    private void addBillItem() {
        try {
            String name = itemNameField.getText().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Item name cannot be empty.");
            }
            double price = Double.parseDouble(priceField.getText().trim());
            String type = (String) itemTypeBox.getSelectedItem();

            BillItem item;

            if ("Shared Appetizer".equals(type)) {
                // Go through every checkbox that belongs to a real friend.
                // If it is ticked, that friend shared this item.
                Friend[] allFriends = friendGroup.getFriends();
                Friend[] buffer = new Friend[allFriends.length];
                int sharerCount = 0;

                for (int i = 0; i < allFriends.length; i++) {
                    if (sharerCheckBoxes[i].isSelected()) {
                        buffer[sharerCount] = allFriends[i];
                        sharerCount++;
                    }
                }

                if (sharerCount == 0) {
                    throw new IllegalArgumentException("Tick at least one person who shared this item.");
                }

                Friend[] sharers = new Friend[sharerCount];
                System.arraycopy(buffer, 0, sharers, 0, sharerCount);
                item = new SharedAppetizer(name, price, sharers);

            } else {
                Friend owner = (Friend) friendSelectBox.getSelectedItem();
                if (owner == null) {
                    throw new IllegalArgumentException("Add at least one friend before adding items.");
                }
                item = new IndividualEntree(name, price, owner);
            }

            friendGroup.addItem(item); // may throw FriendGroupFullException

            itemNameField.setText("");
            priceField.setText("");
            clearSharerCheckboxes();
            refreshTable();

        } catch (NumberFormatException ex) {
            showError("Price must be a valid number.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (FriendGroupFullException ex) {
            showError(ex.getMessage());
        }
    }

    /** Un-ticks every sharer checkbox, ready for the next item. */
    private void clearSharerCheckboxes() {
        for (int i = 0; i < sharerCheckBoxes.length; i++) {
            sharerCheckBoxes[i].setSelected(false);
        }
    }

    /** Applies the selected tip and prints the full breakdown + settlement. */
    private void calculateFinalBill() {
        if (friendGroup.getFriendCount() == 0) {
            showError("Add at least one friend first.");
            return;
        }
        if (friendGroup.getItems().length == 0) {
            showError("Add at least one bill item first.");
            return;
        }

        String tipStr = (String) tipBox.getSelectedItem();
        double tipRate = Double.parseDouble(tipStr.replace("%", "")) / 100.0;

        Object tipSelection = tipPayerBox.getSelectedItem();
        Friend tipPayer = (tipSelection instanceof Friend) ? (Friend) tipSelection : null;
        friendGroup.setTipPayer(tipPayer);

        double subtotal = friendGroup.calculateSubtotal();
        double tax = friendGroup.calculateTotalTax();
        double subtotalWithTax = friendGroup.calculateSubtotalWithTax();
        double tipAmount = friendGroup.calculateTipAmount(tipRate);
        double grandTotal = friendGroup.calculateGrandTotal(tipRate);
        double totalCollected = friendGroup.calculateTotalCollected();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Final Bill Summary ===\n\n");
        sb.append("Items:\n");
        for (BillItem item : friendGroup.getItems()) {
            String kind = (item instanceof SharedAppetizer)
                    ? "shared by " + item.getParticipantsDescription()
                    : "individual - " + item.getParticipantsDescription();
            sb.append(String.format("  %-22s Tk%-8.2f %s%n", item.getItemName(), item.getPrice(), kind));
        }

        sb.append("\n");
        sb.append(String.format("Subtotal (before tax):      Tk%.2f%n", subtotal));
        sb.append(String.format("Tax (%.0f%%):                    Tk%.2f%n", BillItem.getTaxRate() * 100, tax));
        sb.append(String.format("Subtotal (with tax):        Tk%.2f%n", subtotalWithTax));
        sb.append(String.format("Tip (%s, %s):%n    Tk%.2f%n", tipStr,
                tipPayer != null ? "paid entirely by " + tipPayer.getName() : "split proportionally",
                tipAmount));
        sb.append(String.format("Grand Total:                 Tk%.2f%n", grandTotal));
        sb.append(String.format("Total Cash Collected:        Tk%.2f%n", totalCollected));

        if (totalCollected < grandTotal - FriendGroup.EPSILON) {
            sb.append(String.format("!!! WARNING: Total cash collected (Tk%.2f) is less than the grand total (Tk%.2f). Underpayment = Tk%.2f !!!%n",
                    totalCollected, grandTotal, grandTotal - totalCollected));
        } else {
            sb.append(String.format("Leftover Cash to Distribute: Tk%.2f%n", totalCollected - grandTotal));
        }

        sb.append("---------------------------------------------\n");
        sb.append("Per-Friend Breakdown:\n");

        for (Friend f : friendGroup.getFriends()) {
            double food = friendGroup.calculateFriendSubtotal(f);
            double tip = friendGroup.calculateFriendTipShare(f, tipRate);
            double finalAmount = food + tip;
            double change = friendGroup.calculateChange(f, tipRate);

            sb.append(String.format("%-10s food: Tk%-8.2f tip: Tk%-8.2f owes: Tk%-8.2f paid: Tk%-8.2f %s%n",
                    f.getName(), food, tip, finalAmount, f.getCashGiven(),
                    change >= 0
                            ? String.format("(overpaid Tk%.2f)", change)
                            : String.format("(still owes Tk%.2f)", -change)));
        }

        sb.append("---------------------------------------------\n");
        sb.append("Settlement:\n");
        FriendGroup.Transaction[] settlement = friendGroup.computeSettlement(tipRate);
        if (settlement.length == 0) {
            sb.append("  Everyone is settled up exactly -- nothing to send or receive.\n");
        } else {
            for (FriendGroup.Transaction t : settlement) {
                sb.append("  ").append(t).append("\n");
            }
        }

        displayResult(sb.toString());
        refreshTable();
    }

    /** Pushes formatted text into the shared output area. */
    private void displayResult(String text) {
        txtOutput.setText(text);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Friend f : friendGroup.getFriends()) {
            tableModel.addRow(new Object[]{
                    f.getName(),
                    String.format("%.2f", f.getCashGiven()),
                    friendGroup.getItemsInvolving(f).length,
                    String.format("%.2f", friendGroup.calculateFriendSubtotal(f))
            });
        }
    }

    /**
     * Rebuilds everything that lists the current friends: the owner
     * combo box, the tip payer combo box, the user panel combo box,
     * AND turns on the correct sharer checkboxes.
     */
    private void refreshFriendControls() {
        friendSelectBox.removeAllItems();
        userSelectBox.removeAllItems();
        tipPayerBox.removeAllItems();
        tipPayerBox.addItem(TIP_SPLIT_EVENLY);

        Friend[] friends = friendGroup.getFriends();
        for (Friend f : friends) {
            friendSelectBox.addItem(f);
            userSelectBox.addItem(f);
            tipPayerBox.addItem(f);
        }

        // Give each existing friend's checkbox their real name, and
        // update which checkboxes should currently be visible.
        for (int i = 0; i < sharerCheckBoxes.length; i++) {
            if (i < friends.length) {
                sharerCheckBoxes[i].setText(friends[i].getName());
            } else {
                sharerCheckBoxes[i].setText("Friend " + (i + 1));
            }
        }
        updateOwnerArea();
    }


    // =========================================================
    // USER PANEL
    // =========================================================
    private JPanel buildUserPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(900, 680));
        panel.setBackground(Color.WHITE);

        JLabel selectLabel = new JLabel("Select your name:");
        selectLabel.setBounds(10, 15, 130, 25);
        panel.add(selectLabel);

        userSelectBox = new JComboBox<>();
        userSelectBox.setBounds(150, 15, 200, 25);
        panel.add(userSelectBox);

        JButton btnViewBill = new JButton("View My Bill");
        btnViewBill.setBounds(360, 12, 150, 30);
        btnViewBill.addActionListener(e -> viewMyBill());
        panel.add(btnViewBill);

        userOutput = new JTextArea();
        userOutput.setEditable(false);
        userOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(userOutput);
        scroll.setBounds(10, 55, 885, 600);
        panel.add(scroll);

        return panel;
    }

    private void viewMyBill() {
        Friend friend = (Friend) userSelectBox.getSelectedItem();
        if (friend == null) {
            showError("No friend selected. Ask the admin to add you first.");
            return;
        }
        if (friendGroup.getItems().length == 0) {
            showError("No items have been added to the bill yet.");
            return;
        }

        String tipStr = (String) tipBox.getSelectedItem();
        double tipRate = Double.parseDouble(tipStr.replace("%", "")) / 100.0;

        double food = friendGroup.calculateFriendSubtotal(friend);
        double tip = friendGroup.calculateFriendTipShare(friend, tipRate);
        double finalAmount = food + tip;
        double change = friendGroup.calculateChange(friend, tipRate);

        StringBuilder sb = new StringBuilder();
        sb.append("Bill Breakdown for ").append(friend.getName()).append("\n");
        sb.append("---------------------------------------------\n");
        for (BillItem item : friendGroup.getItemsInvolving(friend)) {
            double myShare = item.getShareFor(friend);
            if (item instanceof SharedAppetizer) {
                sb.append(String.format("  %-22s Tk%-8.2f shared %d ways -> your share Tk%.2f%n",
                        item.getItemName(), item.getPrice(), item.getParticipantCount(), myShare));
            } else {
                sb.append(String.format("  %-22s Tk%-8.2f your share (w/ tax) Tk%.2f%n",
                        item.getItemName(), item.getPrice(), myShare));
            }
        }
        sb.append("---------------------------------------------\n");
        sb.append(String.format("Food subtotal (with tax): Tk%.2f%n", food));
        sb.append(String.format("Tip share (%s):           Tk%.2f%n", tipStr, tip));
        sb.append(String.format("Total owed:               Tk%.2f%n", finalAmount));
        sb.append(String.format("Cash given:               Tk%.2f%n", friend.getCashGiven()));
        sb.append(change >= 0
                ? String.format("You overpaid by Tk%.2f.%n", change)
                : String.format("You still owe Tk%.2f.%n", -change));

        sb.append("---------------------------------------------\n");
        sb.append("Settlement involving you:\n");
        FriendGroup.Transaction[] settlement = friendGroup.computeSettlement(tipRate);
        boolean any = false;
        for (FriendGroup.Transaction t : settlement) {
            boolean fromMe = t.getFrom() != null && t.getFrom().equals(friend);
            boolean toMe = t.getTo() != null && t.getTo().equals(friend);
            if (fromMe || toMe) {
                sb.append("  ").append(t).append("\n");
                any = true;
            }
        }
        if (!any) {
            sb.append("  You're settled up exactly -- nothing to send or receive.\n");
        }

        userOutput.setText(sb.toString());
    }

    // ----- Shared helper -----
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BillSplitterGUI gui = new BillSplitterGUI();
            gui.setVisible(true);
        });
    }
}
