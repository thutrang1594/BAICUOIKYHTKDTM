package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.BillAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Bill;

public class BillFragment extends Fragment implements BillAdapter.OnBillListener {

    private RecyclerView rvBills;
    private FloatingActionButton fabAddBill;
    private TextView tvEmptyState;
    private MainActivity activity;
    private BillAdapter billAdapter;
    private List<Bill> billList = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bill, container, false);

        rvBills = view.findViewById(R.id.rv_bills);
        fabAddBill = view.findViewById(R.id.fab_add_bill);
        tvEmptyState = view.findViewById(R.id.tv_empty_bill_state);

        setupRecyclerView();

        fabAddBill.setOnClickListener(v -> showAddEditBillDialog(null));

        updateUI();

        return view;
    }

    private void setupRecyclerView() {
        billAdapter = new BillAdapter(billList, this);
        rvBills.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBills.setAdapter(billAdapter);
    }

    public void updateUI() {
        if (activity != null) {
            this.billList = activity.getBillList();
            billAdapter.updateData(this.billList);
            tvEmptyState.setVisibility(billList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBillChecked(Bill bill, boolean isPaid) {
        bill.setPaid(isPaid);
        if (activity != null) {
            activity.updateBill(bill);
        }
    }

    @Override
    public void onBillLongClicked(Bill bill) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa hóa đơn '" + bill.getName() + "'?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (activity != null) {
                        activity.deleteBill(bill.getId());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAddEditBillDialog(final Bill billToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_bill, null);
        builder.setView(dialogView);

        final EditText etBillName = dialogView.findViewById(R.id.et_bill_name);
        final EditText etBillAmount = dialogView.findViewById(R.id.et_bill_amount);
        final EditText etBillDueDate = dialogView.findViewById(R.id.et_bill_due_date);
        final Spinner spBillCategory = dialogView.findViewById(R.id.spinner_bill_category);
        final EditText etBillNotes = dialogView.findViewById(R.id.et_bill_notes);
        final Button btnSave = dialogView.findViewById(R.id.btn_save_bill);
        final TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);

        // Setup category spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBillCategory.setAdapter(adapter);

        // Setup Date Picker
        final Calendar calendar = Calendar.getInstance();
        etBillDueDate.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                etBillDueDate.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        if (billToEdit != null) {
            tvDialogTitle.setText("Sửa hóa đơn");
            etBillName.setText(billToEdit.getName());
            etBillAmount.setText(String.valueOf(billToEdit.getAmount()));
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            if (billToEdit.getDueDate() != null) {
                etBillDueDate.setText(sdf.format(billToEdit.getDueDate()));
                calendar.setTime(billToEdit.getDueDate());
            }
            etBillNotes.setText(billToEdit.getNotes());
            // Set spinner selection
            if (billToEdit.getCategory() != null) {
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).toString().equals(billToEdit.getCategory())) {
                        spBillCategory.setSelection(i);
                        break;
                    }
                }
            }
        } else {
            tvDialogTitle.setText("Thêm hóa đơn mới");
        }

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String name = etBillName.getText().toString().trim();
            String amountStr = etBillAmount.getText().toString().trim();
            String category = spBillCategory.getSelectedItem().toString();
            String notes = etBillNotes.getText().toString().trim();
            Date dueDate = calendar.getTime();

            if (name.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Tên và số tiền không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            if (billToEdit == null) {
                Bill newBill = new Bill(name, amount, dueDate, category, false, notes);
                activity.addBill(newBill);
            } else {
                billToEdit.setName(name);
                billToEdit.setAmount(amount);
                billToEdit.setDueDate(dueDate);
                billToEdit.setCategory(category);
                billToEdit.setNotes(notes);
                activity.updateBill(billToEdit);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
