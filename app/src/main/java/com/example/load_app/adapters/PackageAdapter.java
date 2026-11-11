package com.example.load_app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.load_app.R;
import com.example.load_app.activities.PackageDetailsActivity;
import com.example.load_app.models.Package;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {

    private List<Package> packageList;
    private OnAssignClickListener assignClickListener;
    private OnPackageClickListener packageClickListener;
    private String userRole;

    public interface OnAssignClickListener {
        void onAssignClick(Package pkg);
    }

    public interface OnPackageClickListener {
        void onPackageClick(Package pkg);
    }

    public PackageAdapter(List<Package> packageList, OnAssignClickListener assignClickListener, String role) {
        this.packageList = packageList;
        this.assignClickListener = assignClickListener;
        this.userRole = role;
    }

    public PackageAdapter(List<Package> packageList, OnPackageClickListener packageClickListener) {
        this.packageList = packageList;
        this.packageClickListener = packageClickListener;
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
        Context context = holder.itemView.getContext();

        holder.tvPackageId.setText(pkg.getPackageId());
        holder.tvDropAddress.setText("Drop: " + pkg.getDropAddress());
        holder.tvWeight.setText(pkg.getWeightKg() + " kg");

        if ("ADMIN".equals(userRole)) {
            holder.tvPrice.setText("৳" + String.format("%.2f", pkg.getCustomerPrice()));
            holder.tvPrice.setVisibility(View.VISIBLE);
        } else if ("RIDER".equals(userRole) || "P2P".equals(userRole)) {
            holder.tvPrice.setText("Income: ৳" + String.format("%.2f", pkg.getRiderFee()));
            holder.tvPrice.setVisibility(View.VISIBLE);
        } else if ("CUSTOMER".equals(userRole)) {
            holder.tvPrice.setText("Cost: ৳" + String.format("%.2f", pkg.getCustomerPrice()));
            holder.tvPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvPrice.setVisibility(View.GONE);
        }

        if (Package.STATUS_DELIVERED.equals(pkg.getStatus())) {
            holder.tvTimer.setVisibility(View.GONE);
        } else {
            holder.tvTimer.setVisibility(View.VISIBLE);
            long elapsed = System.currentTimeMillis() - pkg.getCreatedAt();
            long days = TimeUnit.MILLISECONDS.toDays(elapsed);
            long hours = TimeUnit.MILLISECONDS.toHours(elapsed) % 24;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
            holder.tvTimer.setText(String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds));
        }

        if (pkg.getStatus() != null) {
            holder.tvStatus.setText(pkg.getStatus());
            switch (pkg.getStatus()) {
                case Package.STATUS_PENDING:
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warningOrange));
                    break;
                case Package.STATUS_DELIVERED:
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.successGreen));
                    break;
                default:
                    holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
                    break;
            }
        }

        if ("URGENT".equalsIgnoreCase(pkg.getPriority())) {
            holder.tvPriorityBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvPriorityBadge.setVisibility(View.GONE);
        }

        if ("ADMIN".equals(userRole) && assignClickListener != null && Package.STATUS_PENDING.equals(pkg.getStatus())) {
            holder.btnAssign.setVisibility(View.VISIBLE);
            holder.btnAssign.setOnClickListener(v -> assignClickListener.onAssignClick(pkg));
        } else {
            holder.btnAssign.setVisibility(View.GONE);
        }

        if ("CUSTOMER".equals(userRole)) {
            if (pkg.getPickupOtp() != null && pkg.getDeliveryOtp() != null) {
                holder.tvOtpDisplay.setVisibility(View.VISIBLE);
                holder.tvOtpDisplay.setText("Pickup OTP: " + pkg.getPickupOtp() + "\nDelivery OTP: " + pkg.getDeliveryOtp());
            }
        } else {
            holder.tvOtpDisplay.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if ("ADMIN".equals(userRole)) {
                Intent intent = new Intent(context, PackageDetailsActivity.class);
                intent.putExtra("PACKAGE_ID", pkg.getPackageId());
                context.startActivity(intent);
            } else if (packageClickListener != null) {
                packageClickListener.onPackageClick(pkg);
            }
        });
    }

    @Override
    public int getItemCount() {
        return packageList.size();
    }

    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView tvPackageId, tvStatus, tvDropAddress, tvWeight, tvOtpDisplay, tvPriorityBadge, tvTimer, tvPrice;
        Button btnAssign;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackageId = itemView.findViewById(R.id.tvPackageId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDropAddress = itemView.findViewById(R.id.tvDropAddress);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvOtpDisplay = itemView.findViewById(R.id.tvOtpDisplay);
            tvPriorityBadge = itemView.findViewById(R.id.tvPriorityBadge);
            tvTimer = itemView.findViewById(R.id.tvTimer);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAssign = itemView.findViewById(R.id.btnAssign);
        }
    }
}