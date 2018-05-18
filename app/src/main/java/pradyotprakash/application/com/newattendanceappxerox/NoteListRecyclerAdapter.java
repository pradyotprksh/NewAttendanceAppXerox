package pradyotprakash.application.com.newattendanceappxerox;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoteListRecyclerAdapter extends RecyclerView.Adapter<NoteListRecyclerAdapter.ViewHolder> {

    private List<NoteList> noteList;
    private Context context;
    private String fileLink;
    private long queueId;
    private DownloadManager downloadManager;
    private FirebaseFirestore mFirestore;
    MediaPlayer notifySound;

    public NoteListRecyclerAdapter(List<NoteList> noteList, Context context) {
        this.noteList = noteList;
        this.context = context;
    }

    @Override
    public NoteListRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_list_item, parent, false);
        return new NoteListRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final NoteListRecyclerAdapter.ViewHolder holder, final int position) {
        String uploadedBy = noteList.get(position).getUploadedBy();
        mFirestore = FirebaseFirestore.getInstance();
        mFirestore.collection("Faculty").document(uploadedBy).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        holder.noteUploadedBy.setText(task.getResult().getString("name"));
                    }
                }
            }
        });
        holder.noteName.setText(noteList.get(position).getTitle());
        holder.noteDescription.setText(noteList.get(position).getDescription());
        holder.noteUploadedOn.setText(noteList.get(position).getUploadedOn());
        holder.classvalue.setText(noteList.get(position).getClassValue());
        holder.branch.setText(noteList.get(position).getBranch());
        holder.semester.setText(noteList.get(position).getSemester());
        notifySound = MediaPlayer.create(context, R.raw.notice);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    DownloadManager.Query request_query = new DownloadManager.Query();
                    request_query.setFilterById(queueId);
                    Cursor c = downloadManager.query(request_query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                            String fileUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            File mFile = new File(Uri.parse(fileUri).getPath());
                            String fileName = mFile.getAbsolutePath();
                            try {
                                if (notifySound.isPlaying()) {
                                    notifySound.stop();
                                    notifySound.release();
                                    notifySound = MediaPlayer.create(context, R.raw.notice);
                                } notifySound.start();
                            } catch(Exception e) { e.printStackTrace(); }
                            Toast.makeText(context, "File Stored in: " + fileName, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Download Started. Will Be Notified When Download is Completed.", Toast.LENGTH_SHORT).show();
                File direct = new File(Environment.DIRECTORY_DOWNLOADS
                        + "/Notes"
                        + "/" + noteList.get(position).getBranch()
                        + "/" + noteList.get(position).getSemester()
                        + "/" + noteList.get(position).getClassValue());
                if (!direct.exists()) {
                    direct.mkdirs();
                }
                fileLink = noteList.get(position).getNoteLink();
                downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileLink));
                request.setDestinationInExternalPublicDir(String.valueOf(direct), noteList.get(position).getName());
                queueId = downloadManager.enqueue(request);
            }
        });
        holder.copies.setText(String.valueOf(noteList.get(position).getCopies()));
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public void setfilter(List<NoteList> listitem) {
        noteList = new ArrayList<>();
        noteList.addAll(listitem);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private TextView noteName, noteDescription, noteUploadedOn, noteUploadedBy, branch, semester, classvalue, copies;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            noteName = mView.findViewById(R.id.noteName);
            noteDescription = mView.findViewById(R.id.noteDescription);
            noteUploadedOn = mView.findViewById(R.id.noteUploadedOn);
            noteUploadedBy = mView.findViewById(R.id.noteUploadedBy);
            branch = mView.findViewById(R.id.noteBranch);
            semester = mView.findViewById(R.id.noteSemester);
            classvalue = mView.findViewById(R.id.noteClass);
            copies = mView.findViewById(R.id.noteCopies);
        }
    }
}