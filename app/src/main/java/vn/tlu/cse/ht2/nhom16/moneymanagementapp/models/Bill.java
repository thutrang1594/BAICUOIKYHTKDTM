package vn.tlu.cse.ht2.nhom16.moneymanagementapp.models;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

public class Bill {

    @DocumentId
    private String id;
    private String name;
    private double amount;
    private Date dueDate;
    private String category;
    private boolean isPaid;
    private String notes;

    public Bill() {
        // Default constructor required for calls to DataSnapshot.getValue(Bill.class)
    }

    public Bill(String name, double amount, Date dueDate, String category, boolean isPaid, String notes) {
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.category = category;
        this.isPaid = isPaid;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
