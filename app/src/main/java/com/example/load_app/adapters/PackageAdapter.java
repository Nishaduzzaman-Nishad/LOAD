package com.example.load_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load_app.R;
import com.example.load_app.models.Package;

import java.util.List;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {

    private List<Package> packageList;
    private OnAssignClickListener assignClickListener;
    private OnPackageClickListener packageClickListener;

    public interface OnAssignClickListener {
        void onAssignClick(Package pkg);
    }

    public interface OnPackageClickListener {
        void onPackageClick(Package pkg);
    }

    public PackageAdapter(List<Package> packageList, OnAssignClickListener listener) {
        this.packageList = packageList;
        this.assignClickListener = listener;
    }

    public void setOnPackageClickListener(OnPackageClickListener listener) {
        this.packageClickListener = listener;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        Package pkg = packageList.get(position);

        holder.tvPackageId.setText(pkg.getPackageId());
        holder.tvDropAddress.setText("Drop: " + pkg.getDropAddress());
        holder.tvWeight.setText(pkg.getWeightKg() + " kg");
        holder.tvStatus.setText(pkg.getStatus());

        int colorResId = R.color.primary;
        if (pkg.getStatus().equals(Package.STATUS_DELIVERED)) {
            colorResId = R.color.successGreen;
        } else if (pkg.getStatus().equals(Package.STATUS_PENDING)) {
            colorResId = R.color.accent;
        }
        holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(colorResId));

        if (pkg.getStatus().equals(Package.STATUS_PENDING) && assignClickListener != null) {
            holder.btnAssign.setVisibility(View.VISIBLE);
            holder.btnAssign.setOnClickListener(v -> assignClickListener.onAssignClick(pkg));
            holder.itemView.setOnClickListener(null);
        } else {
            holder.btnAssign.setVisibility(View.GONE);
            if (packageClickListener != null) {
                holder.itemView.setOnClickListener(v -> packageClickListener.onPackageClick(pkg));
            }
        }
    }

    @Override
    public int getItemCount() {
        return packageList.size();
    }

    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView tvPackageId, tvDropAddress, tvWeight, tvStatus;
        Button btnAssign;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackageId = itemView.findViewById(R.id.tvPackageId);
            tvDropAddress = itemView.findViewById(R.id.tvDropAddress);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAssign = itemView.findViewById(R.id.btnAssign);
        }
    }
}