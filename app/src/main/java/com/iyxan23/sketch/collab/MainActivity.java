package com.iyxan23.sketch.collab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;


class MainActivity extends AppCompatActivity {

    // project keys
    // Pair<project_key, "project" file>
    ArrayList<Pair<String, JSONObject>> publicProjectsOwned = new ArrayList<>();
    ArrayList<Pair<String, JSONObject>> userProjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Get rid of the flasing white color while doing shared element transition
        getWindow().setEnterTransition(null);
        getWindow().setExitTransition(null);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if Read and Write external storage permission is granted
        // why scoped storage? :(
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {

            // Tell the user first of why you need to grant the permission
            Toast.makeText(this, "We need storage permission to access your sketchware projects. SketchCollab can misbehave if you denied the permission.", Toast.LENGTH_LONG).show();
            // Reuest the permission(s)
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    100);
        }

        String internalPath = getFilesDir().getAbsolutePath();
        File lastChanges = new File(internalPath + "/last_changes");

        // Get user projects, and projects that the user collaborates
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        FirebaseAuth auth  = FirebaseAuth.getInstance();
        DatabaseReference userProjectsRef = database.getReference("/userprojects/" + auth.getUid());
        DatabaseReference projectsRef = database.getReference("/projects/" + auth.getUid());


        projectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                for (DataSnapshot project : snapshot.getChildren()) {
                    if (project.child("author").getValue(String.class).equals(auth.getUid())) {
                        String projectBase64 = project.child("snapshot").child("project").getValue(String.class);
                        JSONObject json = null;

                        try {
                            json = new JSONObject(Util.base64decode(projectBase64));
                        } catch (JSONException e) {
                            e.printStackTrace();

                            // Something broke
                            return;
                        }

                        // Because project id can vary on different devices
                        json.optInt("sc_id");

                        Pair pair = new Pair(project.getKey(), json);
                        publicProjectsOwned.add(pair);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                            "Error while fetching data: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        userProjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                for (DataSnapshot project : snapshot.getChildren()) {
                    String projectBase64 = project.child("snapshot").child("project").getValue(String.class);

                    JSONObject json = null;
                    try {
                        json = new JSONObject(Util.base64decode(projectBase64));
                    } catch (JSONException e) {
                        e.printStackTrace();

                        // Something broke
                        return;
                    }

                    // Because project id can vary on different devices
                    json.optInt("sc_id");

                    Pair pair = new Pair(project.getKey(), json);
                    userProjects.add(pair);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                                "Error while fetching data: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // OnClicks
        findViewById(R.id.projects_main).setOnClickListener(v -> {

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    MainActivity.this,
                    findViewById(R.id.imageView8),
                    Objects.requireNonNull( ViewCompat.getTransitionName(findViewById(R.id.imageView8)) )
            );

            Intent intent = new Intent(MainActivity.this,SketchwareProjectsListActivity.class);
            startActivity(intent, options.toBundle());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == PackageManager.PERMISSION_GRANTED) {
                // TODO: Fetch sketchware projects
            }
        }
    }
}