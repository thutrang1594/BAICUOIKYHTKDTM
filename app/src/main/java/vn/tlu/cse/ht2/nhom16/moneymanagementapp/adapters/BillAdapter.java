package vn.tlu.cse.ht2.nhom16.moneymanagementapp.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Bill;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<Bill> billList;
    private OnBillListener onBillListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnBillListener {
        void onBillChecked(Bill bill, boolean isPaid);
        void onBillLongClicked(Bill bill);
    }

    public BillAdapter(List<Bill> billList, OnBillListener onBillListener) {
        this.billList = billList;
        this.onBillListener = onBillListener;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = billList.get(position);
        holder.bind(bill, onBillListener);
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public void updateData(List<Bill> newBillList) {
        this.billList = newBillList;
        notifyDataSetChanged();
    }

    class BillViewHolder extends RecyclerView.ViewHolder {

        TextView tvBillName, tvBillDueDate, tvBillAmount;
        CheckBox cbBillPaid;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBillName = itemView.findViewById(R.id.tv_bill_name);
            tvBillDueDate = itemView.findViewById(R.id.tv_bill_due_date);
            tvBillAmount = itemView.findViewById(R.id.tv_bill_amount);
            cbBillPaid = itemView.findViewById(R.id.cb_bill_paid);
        }

        public void bind(final Bill bill, final OnBillListener listener) {
            tvBillName.setText(bill.getName());
            tvBillAmount.setText(String.format(Locale.getDefault(), "%,.0f VND", bill.getAmount()));
            if (bill.getDueDate() != null) {
                tvBillDueDate.setText("Đến hạn: " + dateFormat.format(bill.getDueDate()));
            }

            // Set initial checked state without triggering listener
            cbBillPaid.setOnCheckedChangeListener(null);
            cbBillPaid.setChecked(bill.isPaid());

            // Apply strikethrough effect if paid
            if (bill.isPaid()) {
                tvBillName.setPaintFlags(tvBillName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvBillDueDate.setPaintFlags(tvBillDueDate.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvBillName.setPaintFlags(tvBillName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvBillDueDate.setPaintFlags(tvBillDueDate.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Set the listener
            cbBillPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onBillChecked(bill, isChecked);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onBillLongClicked(bill);
                    return true;
                }
                return false;
            });
        }
    }
}
