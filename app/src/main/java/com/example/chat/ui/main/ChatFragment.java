package com.example.chat.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chat.MainActivity;
import com.example.chat.Message;
import com.example.chat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import static android.app.Activity.RESULT_OK;
import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * A simple {@link Fragment} subclass
 */
public class ChatFragment extends Fragment {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView messengerTextView;
        ImageView messageImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
        }
    }

    private LoginViewModel viewModel;

    private FirebaseDatabase db;

    private LinearLayoutManager linearLayoutManager;

    private RecyclerView mRecyclerView;

    private FirebaseRecyclerAdapter<Message, MessageViewHolder> mFirebaseAdapter;

    private EditText editTextMessage;
    private Button sendButton;
    private ImageView addImageView;

    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db  = FirebaseDatabase.getInstance();

        FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>()
                .setQuery(db.getReference().child("messages"), Message.class)
                .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final MessageViewHolder holder, int position, @NonNull Message model) {
                if (model.getContent() != null) {
                    holder.messageTextView.setText(model.getContent());
                    holder.messageImageView.setVisibility(View.GONE);
                    holder.messageTextView.setVisibility(View.VISIBLE);
                } else if (model.getImgUrl() != null) {
                    String imgUrl = model.getImgUrl();
                    if (imgUrl.startsWith("gs://")) {
                        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imgUrl);
                        storageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(holder.messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(holder.messageImageView);
                                } else {
                                    Log.w("TAG", "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });
                    } else {
                        Glide.with(holder.messageImageView.getContext())
                                .load(imgUrl)
                                .into(holder.messageImageView);
                    }
                    holder.messageImageView.setVisibility(View.VISIBLE);
                    holder.messageTextView.setVisibility(View.GONE);
                }
                holder.messengerTextView.setText(model.getName());
            }

            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false));
            }
        };


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        mRecyclerView = view.findViewById(R.id.messageRecyclerView);
        final NavController navController = Navigation.findNavController(view);
        viewModel.authenticationState.observe(getViewLifecycleOwner(),
                new Observer<LoginViewModel.AuthenticationState>() {
            @Override
            public void onChanged(LoginViewModel.AuthenticationState authenticationState) {
                switch (authenticationState) {
                    case AUTHENTICATED:
                        mFirebaseAdapter.startListening();
                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                        if (!task.isSuccessful()) {
                                            Log.w("TAG", "getInstanceId failed", task.getException());
                                            return;
                                        }

                                        // Get new Instance ID token
                                        String token = task.getResult().getToken();

                                        db.getReference().child("users").child(viewModel.getUid()).setValue(token);

                                        // Log and toast
                                        String msg = token;
                                        Log.d("TAG", msg);
                                        Log.d("TAG", viewModel.getUid());
                                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                        break;
                    case UNAUTHENTICATED:
                        navController.navigate(R.id.login_fragment);
                        break;
                }
            }
        });

        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int mCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (mCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecyclerView.setAdapter(mFirebaseAdapter);

        sendButton = view.findViewById(R.id.sendButton);
        editTextMessage = view.findViewById(R.id.messageEditText);
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(editTextMessage.getText().toString());
                editTextMessage.setText("");
            }
        });

        addImageView = view.findViewById(R.id.addMessageImageView);
        addImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null) {
            final Uri uri = data.getData();
            final Message newMessage = new Message(null, viewModel.getDisplayName(), LOADING_IMAGE_URL);
            db.getReference().child("messages").push().setValue(newMessage, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        String messageId = databaseReference.getKey();
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReference(viewModel.getUid())
                                .child(messageId)
                                .child(uri.getLastPathSegment());

                        uploadImageToStorage(storageRef, uri, messageId, newMessage);
                    } else {
                        Log.w("TAG", "Unable to write message to database.",
                                databaseError.toException());
                    }
                }
            });

        }
    }

    private void send(String message) {
        Message m = new Message(message, viewModel.getDisplayName(), null);
        DatabaseReference ref = db.getReference().child("messages");
        ref.push().setValue(m);
    }

    private void uploadImageToStorage(StorageReference storageReference, Uri uri, final String messageId, final Message old) {
        storageReference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    task.getResult().getMetadata().getReference().getDownloadUrl()
                            .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if(task.isSuccessful()) {
                                        Message finishedMessage = new Message(null, old.getName(), task.getResult().toString());
                                        db.getReference().child("messages").child(messageId).setValue(finishedMessage);
                                    }
                                }
                            });
                } else {
                    Log.w("TAG", "Image upload task was not successful.",
                            task.getException());
                }
            }
        });
    }
}
