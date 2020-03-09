package com.mobile.azrinurvani.rxjavaretrofit.view;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobile.azrinurvani.rxjavaretrofit.R;
import com.mobile.azrinurvani.rxjavaretrofit.network.ApiClient;
import com.mobile.azrinurvani.rxjavaretrofit.network.ApiService;
import com.mobile.azrinurvani.rxjavaretrofit.network.model.Note;
import com.mobile.azrinurvani.rxjavaretrofit.network.model.User;
import com.mobile.azrinurvani.rxjavaretrofit.utils.MyDividerItemDecoration;
import com.mobile.azrinurvani.rxjavaretrofit.utils.PrefUtils;
import com.mobile.azrinurvani.rxjavaretrofit.utils.RecyclerTouchListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.txt_empty_notes_view)
    TextView txtEmptyNotesView;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    private ApiService apiService;
    private CompositeDisposable disposable = new CompositeDisposable();
    private NotesAdapter notesAdapter;
    private List<Note> noteList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.activity_title_home));
        setSupportActionBar(toolbar);

//        white background notification bar
        whitNotificationBar(fab);
        apiService = ApiClient.getClient(getApplicationContext()).create(ApiService.class);

        notesAdapter = new NotesAdapter(this,noteList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this,LinearLayoutManager.VERTICAL,16));
        recyclerView.setAdapter(notesAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {

            }

            @Override
            public void onLongClick(View view, int position) {
                showActionDialog(position);
            }
        }));

        /**
         * Check for stored Api Key in shared preferences
         * If not present, make api call to register the user
         * This will be executed when app is installed for the first time
         * or data is cleared from settings
         * */

        if (TextUtils.isEmpty(PrefUtils.getApiKey(this))){
            registerUser();
        }else{
            // user is already registered, fetch all notes
            fetchAllNotes();
        }

    }


    @OnClick(R.id.fab)
    public void onViewClicked() {
        showNoteDialog(false,null,-1);
    }


    /**
     * Registering new user
     * sending unique id as device identification
     * https://developer.android.com/training/articles/user-data-ids.html
     */
    private void registerUser() {
        //unique id to identify the device
        String uniqueId = UUID.randomUUID().toString();

        disposable.add(
                apiService
                    .register(uniqueId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<User>() {
                        @Override
                        public void onSuccess(User user) {
                            // Storing user API Key in preferences
                            PrefUtils.storeApiKey(getApplicationContext(),user.getApiKey());

                            Toast.makeText(getApplicationContext(),
                                    "Device is registered successfully! ApiKey: " + PrefUtils.getApiKey(getApplicationContext()),
                                    Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG,"RegisterUser.onError : "+e.getMessage());
                            showError(e);
                        }
                    })
        );
    }



/************Method CRUD Note App*************************/


    /**
     * Fetching all notes from api
     * The received items will be in random order
     * map() operator is used to sort the items in descending order by Id
     */
    private void fetchAllNotes(){
        disposable.add(
                apiService.fetchAllNotes()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Function<List<Note>, List<Note>>() {
                        @Override
                        public List<Note> apply(List<Note> notes) throws Exception {
                            // TODO - note about sort
                            Collections.sort(notes, new Comparator<Note>() {
                                @Override
                                public int compare(Note n1, Note n2) {
                                    return n2.getId() - n1.getId();
                                }
                            });
                            return notes;
                        }
                    })
                    .subscribeWith(new DisposableSingleObserver<List<Note>>() {
                        @Override
                        public void onSuccess(List<Note> notes) {
                            noteList.clear();
                            noteList.addAll(notes);
                            notesAdapter.notifyDataSetChanged();

                            toggleEmptyNotes();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG,"FetchingAllNotes.onError : "+e.getMessage());
                            showError(e);
                        }
                    })
        );
    }

    /*
    * Creating new note
    */
    private void createNote(String note) {
        disposable.add(
                apiService.createNote(note)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<Note>() {
                        @Override
                        public void onSuccess(Note note) {
                            if (!TextUtils.isEmpty(note.getError())){
                                Toast.makeText(getApplicationContext(), note.getError(), Toast.LENGTH_LONG).show();
                                return;
                            }

                            Log.d(TAG,"new note created: "+note.getId() + ", "+note.getNote() + ", "+note.getTimestamp());

                            //Add new item and notify adapter
                            noteList.add(0,note);
                            notesAdapter.notifyDataSetChanged();

                            toggleEmptyNotes();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "CreateNote.onError: " + e.getMessage());
                            showError(e);
                        }
                    })
        );
    }

    /**
     * Updating a note
     */
    private void updateNote(int noteId, final String note, final int position) {
        disposable.add(
                apiService.updateNote(noteId,note)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            Log.d(TAG,"Note Updated!");

                            Note n = noteList.get(position);
                            n.setNote(note);

                            // Update item and notify adapter
                            noteList.set(position, n);
                            notesAdapter.notifyItemChanged(position);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "UpdateNote.onError: " + e.getMessage());
                            showError(e);
                        }
                    })
        );
    }

    private void deleteNote(final int noteId,final int position) {
        Log.e(TAG, "deleteNote: " + noteId + ", " + position);
        disposable.add(
                apiService.deleteNote(noteId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            Log.d(TAG, "Note deleted! " + noteId);

                            // Remove and notify adapter about item deletion
                            noteList.remove(position);
                            notesAdapter.notifyDataSetChanged();

                            Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();

                            toggleEmptyNotes();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "deleteNote.onError: " + e.getMessage());
                            showError(e);
                        }
                    })
        );
    }
/************End of Method CRUD Note App*************************/


    private void showNoteDialog(final boolean shouldUpate, final Note note, final int position) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(getApplicationContext());
        View view = layoutInflaterAndroid.inflate(R.layout.note_dialog,null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputNote = view.findViewById(R.id.note);
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        dialogTitle.setText(!shouldUpate ? getString(R.string.lbl_new_note_title): getString(R.string.lbl_new_note_title));

        if (shouldUpate && note != null ){
            inputNote.setText(note.getNote());
        }

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpate ? "update" : "save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogBox, int which) {
                        dialogBox.cancel();
                    }
                });
        final AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast message when no text is entered
                if (TextUtils.isEmpty(inputNote.getText().toString())){
                    Toast.makeText(MainActivity.this, "Enter note!", Toast.LENGTH_SHORT).show();
                    return;
                }else{
                    alertDialog.dismiss();
                }

                // check if user updating note
                if (shouldUpate && note != null){
                    // update note by it's id
                    updateNote(note.getId(),inputNote.getText().toString(),position);
                }else{
                    // create new note
                    createNote(inputNote.getText().toString());
                }
            }
        });

    }

    /**
     * Opens dialog with Edit - Delete options
     * Edit - 0
     * Delete - 0
     */
    private void showActionDialog(final int position){
        CharSequence colors[] = new CharSequence[]{"Edit","Delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option ");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    showNoteDialog(true,noteList.get(position),position);
                }else{
                    deleteNote(noteList.get(position).getId(),position);
                }
            }
        });
        builder.show();
    }



    private void toggleEmptyNotes(){
        if (noteList.size()> 0){
            txtEmptyNotesView.setVisibility(View.GONE);
        }else{
            txtEmptyNotesView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Showing a Snackbar with error message
     * The error body will be in json format
     * {"error": "Error message!"}
     */
    private void showError(Throwable e){
        String message = "";

        try{
            if (e instanceof IOException){
                message = "No Internet Connection !";

            }else if (e instanceof HttpException){
                HttpException error = (HttpException) e;
                String errordBody = error.response().errorBody().string();
                JSONObject jsonObject= new JSONObject(errordBody);

                message = jsonObject.getString("error");
            }

        }catch (IOException ex){
            ex.printStackTrace();

        }catch (JSONException ex){
            ex.printStackTrace();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        if (TextUtils.isEmpty(message)){
            message = "Unknown error occured! Check Logcat !";
        }

        Snackbar snackbar = Snackbar.make(coordinatorLayout, message,Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();

    }

    private void whitNotificationBar(View view){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.WHITE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
}
