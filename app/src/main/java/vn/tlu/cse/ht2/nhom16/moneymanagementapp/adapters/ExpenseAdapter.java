package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private Context context;
    private DecimalFormat decimalFormat;
    private String currentCurrency;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ExpenseAdapter(List<Expense> expenseList, Context context, DecimalFormat decimalFormat, String currentCurrency) {
        this.expenseList = expenseList;
        this.context = context;
        this.decimalFormat = decimalFormat;
        this.currentCurrency = currentCurrency;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvExpenseCategory, tvExpenseDescription, tvExpenseAmount, tvExpenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvExpenseCategory = itemView.findViewById(R.id.tv_expense_category);
            tvExpenseDescription = itemView.findViewById(R.id.tv_expense_description);
            tvExpenseAmount = itemView.findViewById(R.id.tv_expense_amount);
            tvExpenseDate = itemView.findViewById(R.id.tv_expense_date);
        }

        void bind(final Expense expense) {
            tvExpenseCategory.setText(expense.getCategory());
            tvExpenseDescription.setText(expense.getDescription());

            if ("income".equalsIgnoreCase(expense.getType())) {
                tvExpenseAmount.setText(String.format("+%s %s", decimalFormat.format(expense.getAmount()), currentCurrency));
                tvExpenseAmount.setTextColor(ContextCompat.getColor(context, R.color.income_color));
            } else {
                tvExpenseAmount.setText(String.format("-%s %s", decimalFormat.format(expense.getAmount()), currentCurrency));
                tvExpenseAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_color));
            }

            if (expense.getTimestamp() != null) {
                tvExpenseDate.setText(dateFormat.format(expense.getTimestamp()));
            }

            // TODO: Set category icon based on category name
            ivCategoryIcon.setImageResource(R.drawable.ic_category_placeholder);

            itemView.setOnClickListener(v -> showEditDialog(expense));

            itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xóa Giao Dịch")
                        .setMessage("Bạn có chắc muốn xóa giao dịch này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).deleteExpense(expense.getId());
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });
        }
    }

    private void showEditDialog(final Expense expense) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_edit_expense, null);
        builder.setView(dialogView);

        final TextInputEditText etDescription = dialogView.findViewById(R.id.et_edit_description);
        final TextInputEditText etAmount = dialogView.findViewById(R.id.et_edit_amount);
        final RadioGroup rgType = dialogView.findViewById(R.id.rg_edit_type);
        final RadioButton rbIncome = dialogView.findViewById(R.id.rb_edit_income);
        final RadioButton rbExpense = dialogView.findViewById(R.id.rb_edit_expense);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_edit_category);
        final EditText etCustomCategory = dialogView.findViewById(R.id.et_edit_custom_category);

        etDescription.setText(expense.getDescription());
        etAmount.setText(String.valueOf(expense.getAmount()));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        final List<String> incomeCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.income_categories)));
        final List<String> expenseCategories = new ArrayList<>(List.of(context.getResources().getStringArray(R.array.expense_categories)));
        incomeCategories.add("Thêm danh mục mới...");
        expenseCategories.add("Thêm danh mục mới...");

        Runnable updateSpinner = () -> {
            adapter.clear();
            if (rbIncome.isChecked()) {
                adapter.addAll(incomeCategories);
            } else {
                adapter.addAll(expenseCategories);
            }
            adapter.notifyDataSetChanged();

            List<String> currentList = rbIncome.isChecked() ? incomeCategories : expenseCategories;
            int selection = currentList.indexOf(expense.getCategory());
            if (selection != -1) {
                spinnerCategory.setSelection(selection);
                etCustomCategory.setVisibility(View.GONE);
            } else {
                spinnerCategory.setSelection(currentList.size() - 1);
                etCustomCategory.setText(expense.getCategory());
                etCustomCategory.setVisibility(View.VISIBLE);
            }
        };

        if ("income".equalsIgnoreCase(expense.getType())) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        updateSpinner.run();

        rgType.setOnCheckedChangeListener((group, checkedId) -> updateSpinner.run());

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if ("Thêm danh mục mới...".equals(parent.getItemAtPosition(position))) {
                    etCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    etCustomCategory.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        AlertDialog dialog = builder.setTitle("Sửa Giao Dịch")
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newDesc = etDescription.getText().toString().trim();
                String newAmountStr = etAmount.getText().toString().trim();
                if (newDesc.isEmpty() || newAmountStr.isEmpty()) {
                    Toast.makeText(context, "Vui lòng điền đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newType = rbIncome.isChecked() ? "income" : "expense";
                String newCategory;
                if ("Thêm danh mục mới...".equals(spinnerCategory.getSelectedItem().toString())) {
                    newCategory = etCustomCategory.getText().toString().trim();
                    if (newCategory.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập tên danh mục mới", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    newCategory = spinnerCategory.getSelectedItem().toString();
                }

                try {
                    double newAmount = Double.parseDouble(newAmountStr);
                    expense.setDescription(newDesc);
                    expense.setAmount(newAmount);
                    expense.setType(newType);
                    expense.setCategory(newCategory);

                    if (context instanceof MainActivity) {
                        ((MainActivity) context).updateExpense(expense);
                    }
                    dialog.dismiss();

                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
