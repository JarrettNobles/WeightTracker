package com.weighttracker.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying weight entries in the History RecyclerView.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.EntryViewHolder> {

    private Context context;
    private List<WeightEntry> entries;
    private OnDeleteCallback deleteCallback;
    private OnItemClickCallback itemClickCallback;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);

    public interface OnDeleteCallback {
        void onDelete(long entryId);
    }

    public interface OnItemClickCallback {
        void onItemClick(long entryId);
    }

    public HistoryAdapter(Context context, List<WeightEntry> entries,
                          OnDeleteCallback deleteCallback, OnItemClickCallback itemClickCallback) {
        this.context = context;
        this.entries = entries;
        this.deleteCallback = deleteCallback;
        this.itemClickCallback = itemClickCallback;
    }

    /**
     * Updates the entries list and refreshes the adapter.
     */
    public void updateEntries(List<WeightEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @Override
    public EntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EntryViewHolder holder, int position) {
        WeightEntry entry = entries.get(position);
        DataStore dataStore = DataStore.getInstance();

        // Date
        holder.tvDate.setText(DATE_FORMAT.format(entry.getDate()));

        // Show/hide camera icon based on photo
        if (entry.getPhotoPath() != null) {
            holder.ivPhotoIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.ivPhotoIndicator.setVisibility(View.GONE);
        }

        // Weight with unit
        String unit = dataStore.isMetric() ? "kg" : "lbs";
        holder.tvWeight.setText(String.format("%.1f %s", entry.getWeight(), unit));

        // Calculate change from previous entry (next item in list since sorted newest first)
        if (position < entries.size() - 1) {
            WeightEntry previous = entries.get(position + 1);
            double change = entry.getWeight() - previous.getWeight();

            if (change < 0) {
                // Weight loss — red down arrow
                holder.tvArrow.setText("▼");
                holder.tvArrow.setTextColor(ContextCompat.getColor(context, R.color.red_negative));
                holder.tvChange.setText(String.format("-%.1f %s", Math.abs(change), unit));
                holder.tvChange.setTextColor(ContextCompat.getColor(context, R.color.red_negative));
            } else if (change > 0) {
                // Weight gain — green up arrow
                holder.tvArrow.setText("▲");
                holder.tvArrow.setTextColor(ContextCompat.getColor(context, R.color.green_positive));
                holder.tvChange.setText(String.format("+%.1f %s", change, unit));
                holder.tvChange.setTextColor(ContextCompat.getColor(context, R.color.green_positive));
            } else {
                // No change
                holder.tvArrow.setText("—");
                holder.tvArrow.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
                holder.tvChange.setText(String.format("0.0 %s", unit));
                holder.tvChange.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
            }
        } else {
            // First entry ever — no previous to compare
            holder.tvArrow.setText("");
            holder.tvChange.setText("");
            holder.tvChange.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
        }

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteCallback != null) {
                deleteCallback.onDelete(entry.getId());
            }
        });

        // Item click for viewing photo
        holder.itemView.setOnClickListener(v -> {
            if (itemClickCallback != null) {
                itemClickCallback.onItemClick(entry.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    // ─── ViewHolder ─────────────────────────────────────────────────────

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        ImageView ivPhotoIndicator;
        TextView tvWeight;
        TextView tvArrow;
        TextView tvChange;
        ImageButton btnDelete;

        EntryViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_entry_date);
            ivPhotoIndicator = itemView.findViewById(R.id.iv_photo_indicator);
            tvWeight = itemView.findViewById(R.id.tv_entry_weight);
            tvArrow = itemView.findViewById(R.id.tv_entry_arrow);
            tvChange = itemView.findViewById(R.id.tv_entry_change);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
