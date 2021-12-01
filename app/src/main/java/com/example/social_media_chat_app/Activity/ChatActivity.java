package com.example.social_media_chat_app.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_chat_app.Adapter.MessagesAdapter;
import com.example.social_media_chat_app.ModelClass.Messages;
import com.example.social_media_chat_app.R;
import com.example.social_media_chat_app.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    String ReceiversName, ReceiversImage, ReceiversUID, SenderUID;
    CircleImageView profileImage;
    TextView receiversName;
    FirebaseDatabase database;
    FirebaseAuth firebaseAuth;
    public static String sImage;
    public static String rImage;
    ImageView back_btn;

    CardView sendBtn;
    EditText edtMessage;

    String senderRoom, receiverRoom;

    RecyclerView messageAdapter;
    ArrayList<Messages> messagesArrayList;

    MessagesAdapter adapter;
    ActivityChatBinding binding;
    FirebaseStorage storage;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
       // getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.primary_purple));
        }
        database=FirebaseDatabase.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();
        storage=FirebaseStorage.getInstance();

        dialog= new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);


        messagesArrayList=new ArrayList<>();

        ReceiversName=getIntent().getStringExtra("name");
        ReceiversImage=getIntent().getStringExtra("ReceiversImage");
        ReceiversUID=getIntent().getStringExtra("uid");


        profileImage=findViewById(R.id.profile_image);
        receiversName=findViewById(R.id.receiversName);

        messageAdapter=findViewById(R.id.messageAdapter);
        back_btn=findViewById(R.id.back_btn);

        database.getReference().child("presence").child(ReceiversUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status=snapshot.getValue(String.class);
                    if(!status.isEmpty()){
                        if(status.equals("Offline")){
                            binding.receiversStatus.setVisibility(View.GONE);
                        }else{
                            binding.receiversStatus.setText(status);
                            binding.receiversStatus.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        SenderUID=firebaseAuth.getUid();
        senderRoom=SenderUID + ReceiversUID;
        receiverRoom=ReceiversUID + SenderUID;
       adapter= new MessagesAdapter(ChatActivity.this,messagesArrayList,senderRoom, receiverRoom);
        LinearLayoutManager linearLayoutManager= new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageAdapter.setLayoutManager(linearLayoutManager);
       messageAdapter.setAdapter(adapter);


        sendBtn=findViewById(R.id.sendBtn);
        edtMessage=findViewById(R.id.edtMessage);


        Picasso.get().load(ReceiversImage).into(profileImage);
        binding.receiversName.setText(""+ReceiversName);


        DatabaseReference reference= database.getReference().child("user").child(firebaseAuth.getUid());
        DatabaseReference chatReference= database.getReference().child("chats").child(senderRoom).child("messages");


        chatReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesArrayList.clear();
                for(DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Messages messages=dataSnapshot.getValue(Messages.class);
                    messages.setMessageId(snapshot.getKey());
                    messagesArrayList.add(messages);
                }
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sImage=snapshot.child("imageUri").getValue().toString();
                rImage=ReceiversImage;

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=edtMessage.getText().toString();
                if(message.isEmpty()){
                    Toast.makeText(ChatActivity.this, "Please Enter Valid Messages", Toast.LENGTH_SHORT).show();
                    return;
                }
                edtMessage.setText("");
                Date date=new Date();

                Messages messages= new Messages(message, SenderUID, date.getTime());

                String randomKey=database.getReference().push().getKey();

                database=FirebaseDatabase.getInstance();
                assert randomKey != null;
                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(messages).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(messages).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                            }
                        });

                    }
                });
            }
        });
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this,HomeActivity.class));
                Toast.makeText(ChatActivity.this, "Back", Toast.LENGTH_SHORT).show();
            }
        });
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 25);
            }
        });
        final Handler handler= new Handler();
        binding.edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                database.getReference().child("presence").child(SenderUID).setValue("typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping,1000);
            }

            Runnable userStoppedTyping= new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(SenderUID).setValue("Online");
                }
            };
        });


    }
    @Override
    protected void onResume() {
        super.onResume();
        String currentId=FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }
    @Override
    protected void onPause() {
        super.onPause();
        String currentId=FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==25){
            if(data != null){
                if(data.getData()!=null){
                    Uri selectedImage= data.getData();
                    Calendar calendar=Calendar.getInstance();
                    StorageReference reference=storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if(task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath=uri.toString();
                                        String message=edtMessage.getText().toString();
                                     /*   if(message.isEmpty()){
                                            Toast.makeText(ChatActivity.this, "Please Enter Valid Messages", Toast.LENGTH_SHORT).show();
                                            return;
                                        }*/
                                        edtMessage.setText("");
                                        Date date=new Date();

                                        Messages messages= new Messages(message, SenderUID, date.getTime());
                                        messages.setMessage("photo");
                                        messages.setImageUrl(filePath);

                                        String randomKey=database.getReference().push().getKey();

                                        database=FirebaseDatabase.getInstance();
                                        assert randomKey != null;
                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey)
                                                .setValue(messages).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey)
                                                        .setValue(messages).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                    }
                                                });

                                            }
                                        });

                                    //    Toast.makeText(ChatActivity.this, filePath, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }
}