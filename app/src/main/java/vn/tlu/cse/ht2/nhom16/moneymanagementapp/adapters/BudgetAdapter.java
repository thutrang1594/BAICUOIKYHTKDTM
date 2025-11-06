package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgetList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;
    private Map<String, Double> currentMonthExpensesByCategory;

    public BudgetAdapter(List<Budget> budgetList, Context context, DecimalFormat decimalFormat, String currentCurrency) {
        this.budgetList = budgetList;
        this.context = context;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
        this.currentMonthExpensesByCategory = new HashMap<>();
    }

    public void setCurrentMonthExpensesByCategory(Map<String, Double> expenses) {
        this.currentMonthExpensesByCategory = (expenses != null) ? expenses : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);
        holder.bind(budget);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvBudgetCategory, tvBudgetLimit, tvBudgetSpent;
        LinearProgressIndicator progressBudget;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBudgetCategory = itemView.findViewById(R.id.tv_budget_category);
            tvBudgetLimit = itemView.findViewById(R.id.tv_budget_limit);
            tvBudgetSpent = itemView.findViewById(R.id.tv_budget_spent);
            progressBudget = itemView.findViewById(R.id.progress_budget);
        }

        void bind(final Budget budget) {
            tvBudgetCategory.setText(budget.getCategory());

            double spentAmount = currentMonthExpensesByCategory.getOrDefault(budget.getCategory(), 0.0);

            tvBudgetSpent.setText("Đã chi: " + decimalFormat.format(spentAmount));
            tvBudgetLimit.setText(String.format("/ %s %s", decimalFormat.format(budget.getAmount()), currentCurrency));

            int progress = 0;
            if (budget.getAmount() > 0) {
                progress = (int) ((spentAmount / budget.getAmount()) * 100);
            }

            progressBudget.setProgress(Math.min(progress, 100));

            if (spentAmount > budget.getAmount()) {
                progressBudget.setIndicatorColor(ContextCompat.getColor(context, R.color.expense_color));
                tvBudgetSpent.setTextColor(ContextCompat.getColor(context, R.color.expense_color));
            } else {
                progressBudget.setIndicatorColor(ContextCompat.getColor(context, R.color.md_theme_primary));
                tvBudgetSpent.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant));
            }

            itemView.setOnClickListener(v -> showEditDialog(budget));

            itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xóa Ngân sách")
                        .setMessage("Bạn có muốn xóa ngân sách cho '" + budget.getCategory() + "'?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).deleteBudget(budget.getId());
                                Snackbar.make(itemView, "Đã xóa ngân sách", Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });
        }
    }

    private void showEditDialog(Budget budget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_budget, null);
        builder.setView(dialogView);

        Spinner spinnerEditBudgetCategory = dialogView.findViewById(R.id.spinner_edit_budget_category);
        EditText etEditBudgetCustomCategory = dialogView.findViewById(R.id.et_edit_budget_custom_category);
        EditText etEditAmount = dialogView.findViewById(R.id.et_edit_budget_amount);

        List<String> expenseCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.expense_categories)));
        expenseCategories.add("Thêm danh mục mới...");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, expenseCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditBudgetCategory.setAdapter(categoryAdapter);

        int selectionIndex = expenseCategories.indexOf(budget.getCategory());
        if (selectionIndex != -1) {
            spinnerEditBudgetCategory.setSelection(selectionIndex);
            etEditBudgetCustomCategory.setVisibility(View.GONE);
        } else {
            spinnerEditBudgetCategory.setSelection(expenseCategories.size() - 1);
            etEditBudgetCustomCategory.setVisibility(View.VISIBLE);
            etEditBudgetCustomCategory.setText(budget.getCategory());
        }

        spinnerEditBudgetCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("Thêm danh mục mới...".equals(parent.getItemAtPosition(position).toString())) {
                    etEditBudgetCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etEditBudgetCustomCategory.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etEditAmount.setText(String.valueOf(budget.getAmount()));

        builder.setTitle("Sửa Ngân sách")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newCategory;
                    boolean isCustomCategory = "Thêm danh mục mới...".equals(spinnerEditBudgetCategory.getSelectedItem().toString());
                    if (isCustomCategory) {
                        newCategory = etEditBudgetCustomCategory.getText().toString().trim();
                    } else {
                        newCategory = spinnerEditBudgetCategory.getSelectedItem().toString();
                    }

                    if (newCategory.isEmpty()) {
                        Toast.makeText(context, "Danh mục không được để trống.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newAmountStr = etEditAmount.getText().toString().trim();
                    if (newAmountStr.isEmpty()) {
                        Toast.makeText(context, "Số tiền không được để trống.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double newAmount = Double.parseDouble(newAmountStr);
                        budget.setCategory(newCategory);
                        budget.setAmount(newAmount);
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).updateBudget(budget);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Số tiền không hợp lệ.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null);

        builder.create().show();
    }
}
