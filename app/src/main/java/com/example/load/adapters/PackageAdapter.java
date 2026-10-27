package com.example.load.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.load.R;
import com.example.load.models.Package;

import java.util.List;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {

    private List<Package> packageList;

    public PackageAdapter(List<Package> packageList) {
        this.packageList = packageList;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        Package pkg = packageList.get(position);

        holder.tvPackageId.setText(pkg.getPackageId());
        holder.tvDropAddress.setText("Drop: " + pkg.getDropAddress());
        holder.tvWeight.setText(pkg.getWeightKg() + " kg");
        holder.tvStatus.setText(pkg.getStatus());

        // Add this color logic
        int colorResId;
        if (pkg.getStatus().equals(Package.STATUS_DELIVERED)) {
            colorResId = R.color.successGreen;
        } else if (pkg.getStatus().equals(Package.STATUS_PENDING)) {
            colorResId = R.color.accent; // Yellow
        } else {
            colorResId = R.color.primary; // Navy
        }
        holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(colorResId));
    }

    @Override
    public int getItemCount() {
        return packageList.size();
    }

    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView tvPackageId, tvDropAddress, tvWeight, tvStatus;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPackageId = itemView.findViewById(R.id.tvPackageId);
            tvDropAddress = itemView.findViewById(R.id.tvDropAddress);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}