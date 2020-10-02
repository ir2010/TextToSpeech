package com.ir_sj.chatlive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.DateFormat;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int SIGN_IN_REQUEST_CODE = 1;
    private FirebaseListAdapter<ChatMessage> adapter;
    FloatingActionButton fab;
    private static int RESULT_LOAD_IMAGE = 2;
    private Uri filePath;
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    FirebaseStorage storage;
    StorageReference storageReference, sref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText message = (EditText)findViewById(R.id.input);
                Toast.makeText(MainActivity.this, "Sent!", Toast.LENGTH_SHORT).show();

                ref.push().setValue(new ChatMessage(message.getText().toString(),
                                FirebaseAuth.getInstance().getCurrentUser()
                        .getDisplayName())
                        );
                msg.setText("");
            }
        });


        if(FirebaseAuth.getInstance().getCurrentUser() == null) //no user signed in
        {
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder().build(),
                    SIGN_IN_REQUEST_CODE);
        }
        else
        {
            Toast.makeText(this, "Welcome "+FirebaseAuth.getInstance().
                    getCurrentUser().getDisplayName(), Toast.LENGTH_SHORT).show();
            displayChat();
        }
        storageReference = FirebaseStorage.getInstance().getReference();
        sref = storageReference.child("images/" + UUID.randomUUID().toString());
        //downloadImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                Toast.makeText(this, "Successfully signed in!", Toast.LENGTH_SHORT).show();
                displayChat();
            }
            else
            {
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data && data.getData() != null)
        {
            filePath = data.getData();
            ImageView imgView = findViewById(R.id.imgView);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imgView.setImageBitmap(bitmap);
                uploadImage();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menu_sign_out)
        {
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "Signed Out!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        }

        if(item.getItemId() == R.id.menu_change_bg)
        {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(i, "Select Picture"), RESULT_LOAD_IMAGE);
        }
        return true;
    }

    private void displayChat()
    {
        ListView listOfMsgs = (ListView)findViewById(R.id.list_of_messages);
        adapter = new FirebaseListAdapter<ChatMessage>(this, ChatMessage.class, R.layout.message, ref) {
            @Override
            protected void populateView(View v, ChatMessage model, int position) {
                TextView msgText = (TextView) v.findViewById(R.id.message_text);
                TextView msgUser = (TextView) v.findViewById(R.id.message_user);
                TextView msgTime = (TextView) v.findViewById(R.id.message_time);

                msgText.setText(model.getMessageText());
                msgUser.setText(model.getMessageUser());
                //msgTime.setText(String.valueOf(model.getMessageTime()));
            }
        };
        listOfMsgs.setAdapter(adapter);
    }

    private void uploadImage()
    {
       if(filePath != null)
       {
           final ProgressDialog progressDialog = new ProgressDialog(this);
           progressDialog.setTitle("Uploading...");
           progressDialog.show();

           sref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
               @Override
               public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
               {
                   progressDialog.dismiss();
                   Toast.makeText(MainActivity.this, "Uploaded!", Toast.LENGTH_SHORT).show();
               }
           }).addOnFailureListener(new OnFailureListener()
           {
               @Override
               public void onFailure(@NonNull Exception e)
               {
                   progressDialog.dismiss();
                   Toast.makeText(MainActivity.this, "Failed!"+e.getMessage(), Toast.LENGTH_SHORT).show();
               }
           }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>()
           {
               @Override
               public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                   double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                   progressDialog.setMessage("Uploaded "+(int)progress+"%");
               }
           });
       }
    }

    private void downloadImage()
    {
        sref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();
                DownloadManager dm = (DownloadManager)MainActivity.this.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri u = Uri.parse(url);
                DownloadManager.Request request = new DownloadManager.Request(u);

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                //request.setDestinationInExternalFilesDir(MainActivity.this, ,"")
                dm.enqueue(request);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }
}
