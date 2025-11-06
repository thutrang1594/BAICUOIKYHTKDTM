package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters.ExpenseAdapter;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense;
    private RecyclerView rvRecentExpenses;
    private FloatingActionButton fabAddExpense;
    private ExpenseAdapter recentExpensesAdapter;
    private List<Expense> recentExpenseList = new ArrayList<>();

    private MainActivity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString() + " must be MainActivity");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);
        fabAddExpense = view.findViewById(R.id.fab_add_expense);

        setupRecyclerView();

        fabAddExpense.setOnClickListener(v -> showAddTransactionDialog());

        updateUI();

        return view;
    }

    private void setupRecyclerView() {
        recentExpensesAdapter = new ExpenseAdapter(recentExpenseList, getContext(), activity.getDecimalFormat(), activity.getCurrentCurrency());
        rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentExpenses.setAdapter(recentExpensesAdapter);
    }

    public void updateUI() {
        if (activity == null || !isAdded()) return;

        List<Expense> allExpenses = activity.getExpenseList();
        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;

        for (Expense expense : allExpenses) {
            if (expense.getType().equalsIgnoreCase("income")) {
                totalIncome += expense.getAmount();
            } else {
                totalExpense += expense.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;

        tvTotalBalance.setText(String.format("%s %s", decimalFormat.format(balance), currency));
        tvTotalIncome.setText(decimalFormat.format(totalIncome));
        tvTotalExpense.setText(decimalFormat.format(totalExpense));

        // Update recent transactions list (e.g., show latest 5)
        recentExpenseList.clear();
        int limit = Math.min(allExpenses.size(), 5);
        for (int i = 0; i < limit; i++) {
            recentExpenseList.add(allExpenses.get(i));
        }
        recentExpensesAdapter.notifyDataSetChanged();
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        final TextInputEditText etDescription = dialogView.findViewById(R.id.et_dialog_description);
        final TextInputEditText etAmount = dialogView.findViewById(R.id.et_dialog_amount);
        final RadioGroup rgType = dialogView.findViewById(R.id.rg_dialog_type);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_dialog_category);
        final Button btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        // Setup Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String description = etDescription.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            int selectedTypeId = rgType.getCheckedRadioButtonId();
            String type = (selectedTypeId == R.id.rb_dialog_income) ? "income" : "expense";

            if (description.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                Expense newExpense = new Expense(description, amount, type, category);
                activity.addExpense(newExpense);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
