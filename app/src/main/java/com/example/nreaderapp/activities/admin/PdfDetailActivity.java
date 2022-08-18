package com.example.nreaderapp.activities.admin;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.nreaderapp.MyApplication;
import com.example.nreaderapp.R;
import com.example.nreaderapp.databinding.ActivityPdfDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PdfDetailActivity extends AppCompatActivity {

    String bookId, bookTitle, bookUrl;
    String TAG_DOWLOAD = "DOWLOAD_TAG";
    boolean isInMyFavorites = false;
    private FirebaseAuth firebaseAuth;
    private ActivityPdfDetailBinding binding;
    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->{
        if(isGranted){
            Log.d(TAG_DOWLOAD, " Permission granted");
            MyApplication.dowloadBook(this, "" + bookId, "" + bookId, "" + bookUrl);
        }else {
            Log.d(TAG_DOWLOAD, ": Access denied");
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
        }
    });

    private void loadBookDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
        reference.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookTitle = "" + snapshot.child("title").getValue();
                String description = "" + snapshot.child("description").getValue();
                String categoryId = "" + snapshot.child("categoryId").getValue();
                String viewsCount = "" + snapshot.child("viewsCount").getValue();
                String dowloadsCount = "" + snapshot.child("dowloadsCount").getValue();
                bookUrl = "" + snapshot.child("url").getValue();
                String timestamp = "" + snapshot.child("timestamp").getValue();

                binding.dowloadBookBtn.setVisibility(View.VISIBLE);

                String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                MyApplication.loadCategory("" + categoryId, binding.categoryTv);
                MyApplication.loadPdfFromUrlSinglePage("" + bookUrl, "" + bookTitle, binding.pdfView, binding.progressBar, binding.pagesTv);
                MyApplication.loadPdfSize("" + bookUrl, "" + bookTitle, binding.sizeTv);

                binding.sizeTv.setText(bookTitle);
                binding.descriptionTv.setText(description);
                binding.viewsTv.setText(viewsCount.replace("null", "N/a"));
                binding.dowloadsTv.setText(dowloadsCount.replace("null", "N/a"));
                binding.dateTv.setText(date);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkIsFavorite(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Favorites").child(bookId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isInMyFavorites = snapshot.exists();
                if (isInMyFavorites){
                    binding.favoriteBookBtn.setText("Remove favorite");
                }else {
                    binding.favoriteBookBtn.setText("Add favorite");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        loadBookDetails();
        MyApplication.incrementBookViewCount(bookId);
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
            checkIsFavorite();
        }

        binding.dowloadBookBtn.setVisibility(View.GONE);

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentPdfView = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intentPdfView.putExtra("bookId", bookId);
                startActivity(intentPdfView);
            }
        });

        binding.dowloadBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG_DOWLOAD, "onClick: Checking permission");
                if (ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG_DOWLOAD, "onClick: Permission granted, process dowloading");
                    MyApplication.dowloadBook(PdfDetailActivity.this, "" + bookId, "" + bookTitle, "" + bookUrl);

                }else {
                    Log.d(TAG_DOWLOAD, "onClick: Permission wasn't granted");
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        binding.favoriteBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this, "You are not logged in", Toast.LENGTH_SHORT).show();
                }else {
                    if (isInMyFavorites){
                        MyApplication.removeFromFavorite(PdfDetailActivity.this, bookId);
                    }else {
                        MyApplication.addToFavorite(PdfDetailActivity.this, bookId);
                    }
                }
            }
        });
    }


}