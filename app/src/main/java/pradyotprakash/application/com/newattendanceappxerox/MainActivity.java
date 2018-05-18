package pradyotprakash.application.com.newattendanceappxerox;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView noteListRecycler;
    private List<NoteList> notesList;
    private NoteListRecyclerAdapter noteRecyclerAdapter;
    private FirebaseFirestore mFirestore;
    private SearchView searchView;
    private SwipeRefreshLayout refreshPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar mToolbar = findViewById(R.id.uploadNotesToolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Notes List");
        noteListRecycler = findViewById(R.id.noteList);
        notesList = new ArrayList<>();
        noteRecyclerAdapter = new NoteListRecyclerAdapter(notesList, getApplicationContext());
        noteListRecycler.setHasFixedSize(true);
        noteListRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        noteListRecycler.setAdapter(noteRecyclerAdapter);
        mFirestore = FirebaseFirestore.getInstance();
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(noteListRecycler.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(MainActivity.this, R.drawable.horizontal_divider);
        horizontalDecoration.setDrawable(horizontalDivider);
        noteListRecycler.addItemDecoration(horizontalDecoration);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.
                    checkSelfPermission(MainActivity.this,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.
                            checkSelfPermission(MainActivity.this,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                notesList.clear();
                mFirestore.collection("Notes").addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        for (DocumentChange documentChange : documentSnapshots.getDocumentChanges()) {
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                String noteId = documentChange.getDocument().getId();
                                NoteList noteList = documentChange.getDocument().toObject(NoteList.class).withId(noteId);
                                notesList.add(noteList);
                                noteRecyclerAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
            }
        } else {
            notesList.clear();
            mFirestore.collection("Notes").addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    for (DocumentChange documentChange : documentSnapshots.getDocumentChanges()) {
                        if (documentChange.getType() == DocumentChange.Type.ADDED) {
                            String noteId = documentChange.getDocument().getId();
                            NoteList noteList = documentChange.getDocument().toObject(NoteList.class).withId(noteId);
                            notesList.add(noteList);
                            noteRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }
        refreshPage = findViewById(R.id.refreshLayout);
        refreshPage.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mFirestore.collection("Notes").addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        notesList.clear();
                        for (DocumentChange documentChange : documentSnapshots.getDocumentChanges()) {
                            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                                String noteId = documentChange.getDocument().getId();
                                NoteList noteList = documentChange.getDocument().toObject(NoteList.class).withId(noteId);
                                notesList.add(noteList);
                                noteRecyclerAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshPage.setRefreshing(false);
                        Toast.makeText(MainActivity.this, "Refreshed.", Toast.LENGTH_SHORT).show();
                    }
                }, 4000);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.searchfile, menu);
        final MenuItem myActionMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) myActionMenuItem.getActionView();
        changeSearchViewTextColor(searchView);
        ((EditText) searchView.findViewById(
                android.support.v7.appcompat.R.id.search_src_text)).
                setHintTextColor(getResources().getColor(R.color.colorPrimary));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                myActionMenuItem.collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Toast.makeText(MainActivity.this, "Search Branch.", Toast.LENGTH_SHORT).show();
                final List<NoteList> filtermodelist = filter(notesList, newText);
                noteRecyclerAdapter.setfilter(filtermodelist);
                return true;
            }
        });
        return true;
    }

    private List<NoteList> filter(List<NoteList> pl, String query) {
        query = query.toLowerCase();
        final List<NoteList> filteredModeList = new ArrayList<>();
        for (NoteList model : pl) {
            final String text = model.getBranch().toLowerCase();
            if (text.startsWith(query)) {
                filteredModeList.add(model);
            }
        }
        return filteredModeList;
    }

    private void changeSearchViewTextColor(View view) {
        if (view != null) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(Color.WHITE);
                return;
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    changeSearchViewTextColor(viewGroup.getChildAt(i));
                }
            }
        }
    }
}
