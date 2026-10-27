package com.example.load.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.load.models.Package;
import java.util.List;

@Dao
public interface PackageDao {

    @Insert
    void insert(Package pkg);

    @Query("SELECT * FROM packages ORDER BY createdAt DESC")
    List<Package> getAllPackages();

    @Query("SELECT * FROM packages WHERE status = :status")
    List<Package> getPackagesByStatus(String status);
}