package vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.AiInsightsFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.BillFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.BudgetFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.HistoryFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.HomeFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments.StatisticsFragment;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Bill;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Budget;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private String userId;

    private BottomNavigationView bottomNavigationView;

    private List<Expense> expenseList;
    private List<Budget> budgetList;
    private List<Bill> billList;
    private SharedPreferences sharedPreferences;
    private String currentCurrency = "VND";
    private DecimalFormat decimalFormat;

    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private StatisticsFragment statisticsFragment;
    private AiInsightsFragment aiInsightsFragment;
    private BudgetFragment budgetFragment;
    private BillFragment billFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        updateDecimalFormat();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();

        expenseList = new ArrayList<>();
        budgetList = new ArrayList<>();
        billList = new ArrayList<>();

        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment();
        statisticsFragment = new StatisticsFragment();
        aiInsightsFragment = new AiInsightsFragment();
        budgetFragment = new BudgetFragment();
        billFragment = new BillFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(historyFragment);
                return true;
            } else if (itemId == R.id.nav_statistics) {
                loadFragment(statisticsFragment);
                return true;
            } else if (itemId == R.id.nav_budget) {
                loadFragment(budgetFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        listenForExpenses();
        listenForBudgets();
        listenForBills();
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right
        );

        if (activeFragment != null && activeFragment != fragment) {
            fragmentTransaction.hide(activeFragment);
        }

        if (fragmentManager.findFragmentByTag(fragment.getClass().getSimpleName()) == null) {
            fragmentTransaction.add(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
        }

        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();
        activeFragment = fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_sign_out) {
            signOut();
            return true;
        } else if (itemId == R.id.action_bills) {
            loadFragment(billFragment);
            return true;
        } else if (itemId == R.id.action_ai_insights) {
            loadFragment(aiInsightsFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Getter methods for fragments
    public List<Expense> getExpenseList() { return expenseList; }
    public List<Budget> getBudgetList() { return budgetList; }
    public List<Bill> getBillList() { return billList; }
    public DecimalFormat getDecimalFormat() { return decimalFormat; }
    public String getCurrentCurrency() { return currentCurrency; }

    private void updateDecimalFormat() {
        decimalFormat = new DecimalFormat("#,##0");
    }

    //region CRUD Operations for Expenses
    public void addExpense(Expense expense) {
        if (expense.getTimestamp() == null) {
            expense.setTimestamp(new Date());
        }
        db.collection("users").document(userId).collection("expenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> Snackbar.make(findViewById(android.R.id.content), "Đã thêm giao dịch.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi thêm giao dịch.", Snackbar.LENGTH_LONG).show());
    }

    public void updateExpense(Expense expense) {
        if (expense.getId() == null) return;
        db.collection("users").document(userId).collection("expenses").document(expense.getId())
                .set(expense)
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã cập nhật giao dịch.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi cập nhật giao dịch.", Snackbar.LENGTH_LONG).show());
    }

    public void deleteExpense(String expenseId) {
        db.collection("users").document(userId).collection("expenses").document(expenseId)
                .delete()
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã xóa giao dịch.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi xóa giao dịch.", Snackbar.LENGTH_LONG).show());
    }
    //endregion

    //region CRUD Operations for Budgets
    public void addBudget(Budget budget) {
        db.collection("users").document(userId).collection("budgets")
                .add(budget)
                .addOnSuccessListener(documentReference -> Snackbar.make(findViewById(android.R.id.content), "Đã thêm ngân sách.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi thêm ngân sách.", Snackbar.LENGTH_LONG).show());
    }

    public void updateBudget(Budget budget) {
        if (budget.getId() == null) return;
        db.collection("users").document(userId).collection("budgets").document(budget.getId())
                .set(budget)
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã cập nhật ngân sách.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi cập nhật ngân sách.", Snackbar.LENGTH_LONG).show());
    }

    public void deleteBudget(String budgetId) {
        db.collection("users").document(userId).collection("budgets").document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã xóa ngân sách.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi xóa ngân sách.", Snackbar.LENGTH_LONG).show());
    }
    //endregion

    // Firestore listeners
    private void listenForExpenses() {
        db.collection("users").document(userId).collection("expenses")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for expenses.", error);
                        return;
                    }
                    expenseList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Expense expense = doc.toObject(Expense.class);
                            expense.setId(doc.getId());
                            expenseList.add(expense);
                        }
                    }
                    if (homeFragment != null && homeFragment.isAdded()) homeFragment.updateUI();
                    if (historyFragment != null && historyFragment.isAdded()) historyFragment.updateUI();
                    if (statisticsFragment != null && statisticsFragment.isAdded()) statisticsFragment.updateUI();
                    if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI();
                });
    }

    private void listenForBudgets() {
        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);
        db.collection("users").document(userId).collection("budgets")
                .whereEqualTo("monthYear", currentMonthYear)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for budgets.", error);
                        return;
                    }
                    budgetList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Budget budget = doc.toObject(Budget.class);
                            budget.setId(doc.getId());
                            budgetList.add(budget);
                        }
                    }
                    if (budgetFragment != null && budgetFragment.isAdded()) budgetFragment.updateUI();
                });
    }

    private void listenForBills() {
        db.collection("users").document(userId).collection("bills")
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for bills.", error);
                        return;
                    }
                    billList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Bill bill = doc.toObject(Bill.class);
                            bill.setId(doc.getId());
                            billList.add(bill);
                        }
                    }
                    if (billFragment != null && billFragment.isAdded()) {
                        billFragment.updateUI();
                    }
                });
    }

    // CRUD Operations for Bills
    public void addBill(Bill bill) {
        db.collection("users").document(userId).collection("bills")
                .add(bill)
                .addOnSuccessListener(documentReference -> Snackbar.make(findViewById(android.R.id.content), "Đã thêm hóa đơn.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi thêm hóa đơn.", Snackbar.LENGTH_LONG).show());
    }

    public void updateBill(Bill bill) {
        if (bill.getId() == null) return;
        db.collection("users").document(userId).collection("bills").document(bill.getId())
                .set(bill)
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã cập nhật hóa đơn.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi cập nhật hóa đơn.", Snackbar.LENGTH_LONG).show());
    }

    public void deleteBill(String billId) {
        db.collection("users").document(userId).collection("bills").document(billId)
                .delete()
                .addOnSuccessListener(aVoid -> Snackbar.make(findViewById(android.R.id.content), "Đã xóa hóa đơn.", Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Snackbar.make(findViewById(android.R.id.content), "Lỗi khi xóa hóa đơn.", Snackbar.LENGTH_LONG).show());
    }

    public void saveBudgetFromAI(JSONArray budgetData) {
        Calendar cal = Calendar.getInstance();
        long currentMonthYear = cal.get(Calendar.YEAR) * 100L + (cal.get(Calendar.MONTH) + 1);
        db.collection("users").document(userId).collection("budgets")
                .whereEqualTo("monthYear", currentMonthYear)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch deleteBatch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        deleteBatch.delete(doc.getReference());
                    }
                    deleteBatch.commit().addOnSuccessListener(aVoid -> {
                        WriteBatch addBatch = db.batch();
                        for (int i = 0; i < budgetData.length(); i++) {
                            try {
                                JSONObject budgetItem = budgetData.getJSONObject(i);
                                String category = budgetItem.getString("category");
                                double amount = budgetItem.getDouble("amount");
                                Budget budget = new Budget(category, amount, currentMonthYear);
                                DocumentReference budgetRef = db.collection("users").document(userId)
                                        .collection("budgets").document();
                                addBatch.set(budgetRef, budget);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing budget item from AI", e);
                            }
                        }
                        addBatch.commit().addOnSuccessListener(aVoid1 -> {
                            Snackbar.make(findViewById(android.R.id.content), "AI đã cập nhật ngân sách thành công!", Snackbar.LENGTH_LONG).show();
                            bottomNavigationView.setSelectedItemId(R.id.nav_budget);
                        }).addOnFailureListener(e -> Log.e(TAG, "Error committing add batch from AI", e));
                    }).addOnFailureListener(e -> Log.e(TAG, "Error committing delete batch for old budgets", e));
                }).addOnFailureListener(e -> Log.e(TAG, "Error fetching old budgets to delete", e));
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}
