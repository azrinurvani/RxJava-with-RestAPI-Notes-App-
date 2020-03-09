package com.mobile.azrinurvani.rxjavaretrofit.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v7.widget.RecyclerView;


import com.mobile.azrinurvani.rxjavaretrofit.R;
import com.mobile.azrinurvani.rxjavaretrofit.network.model.Note;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.MyViewHolder> {

    private Context context;
    private List<Note> notesList;

    public NotesAdapter(Context context, List<Note> notesList) {
        this.context = context;
        this.notesList = notesList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Note note = notesList.get(position);

        holder.note.setText(note.getNote());

        // Displaying dot from HTML character code
        holder.dot.setText(Html.fromHtml("&#8226;"));

        // Changing dot color to random color
        holder.dot.setTextColor(getRandomMaterialColor("400"));

        // Formatting and displaying timestamp
        holder.timestamp.setText(formatDate(note.getTimestamp()));


    }


    @Override
    public int getItemCount() {
        return notesList.size();
    }

    private int getRandomMaterialColor(String typedColor) {
        int returnColor = Color.GRAY;
        int arrayId = context.getResources().getIdentifier("mdcolor_"+typedColor,"array",context.getPackageName());

        if (arrayId!=0){
            TypedArray colors = context.getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());

            returnColor = colors.getColor(index,Color.GRAY);
            colors.recycle();
        }

        return returnColor;
    }

    private String formatDate(String dateStr) {

        try{
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = fmt.parse(dateStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat("MMM d");
            return fmtOut.format(date);

        }catch (ParseException e){
            Log.e("NoteApp","NotesAdapter.formatDate exception : "+e);
        }

        return "";
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.dot)
        TextView dot;
        @BindView(R.id.timestamp)
        TextView timestamp;
        @BindView(R.id.note)
        TextView note;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
