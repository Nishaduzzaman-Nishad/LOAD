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
    private String userRole; // NEW: To track who is viewing the list

    public interface OnAssignClickListener {
        void onAssignClick(Package pkg);
    }

    public interface OnPackageClickListener {
        void onPackageClick(Package pkg);
    }

    // Updated Constructor to take userRole
    public PackageAdapter(List<Package> packageList, OnAssignClickListener listener, String userRole) {
        this.packageList = packageList;
        this.assignClickListener = listener;
        this.userRole = userRole;
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

        // Show OTP only to the Customer
        if ("CUSTOMER".equals(userRole) && pkg.getOtp() != null) {
            holder.tvOtpDisplay.setVisibility(View.VISIBLE);
            holder.tvOtpDisplay.setText("OTP: " + pkg.getOtp());
        } else {
            holder.tvOtpDisplay.setVisibility(View.GONE);
        }

        if (pkg.getStatus().equals(Package.STATUS_PENDING) && assignClickListener != null) {
            holder.btnAssign.setVisibility(View.VISIBLE);

            // Change button text based on role
            if ("P2P".equals(userRole)) {
                holder.btnAssign.setText("ACCEPT");
            } else {
                holder.btnAssign.setText("ASSIGN");
            }

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
        TextView tvPackageId, tvDropAddress, tvWeight, tvStatus, tvOtpDisplay;
        Button btnAssign;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackageId = itemView.findViewById(R.id.tvPackageId);
            tvDropAddress = itemView.findViewById(R.id.tvDropAddress);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOtpDisplay = itemView.findViewById(R.id.tvOtpDisplay); // Added to XML below
            btnAssign = itemView.findViewById(R.id.btnAssign);
        }
    }
}