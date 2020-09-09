package com.ihsan.sketch.collab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.transition.Explode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private DrawerLayout drawerLayout;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private ArrayList<SketchwareProject> sketchwareProjects = new ArrayList<>();
    private ArrayList<OnlineProject> onlineProjects = new ArrayList<>();
    private ArrayList<OnlineProject> allOnlineProjects = new ArrayList<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setExitTransition(new Explode());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        final FirebaseUser user = auth.getCurrentUser();
        final String[] name = new String[1];

        drawerLayout = findViewById(R.id.drawer_layout);
        final NavigationView nv = findViewById(R.id.navview);

        // Check storage perms
        storagePerms();
        Util.localProjects = sketchwareProjects;

        if (user == null) {
            // Mod user detected
            String hey = "HEY YOU, DON'T YOU DARE MAKE A MOD OF THIS APP";
            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        }

        final MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Home");

        final View.OnClickListener changeName = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeName();
            }
        };

        database.getReference("/")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Currently inefficient, finding for another alternative
                        DataSnapshot userdata = snapshot.child("userdata").child(user.getUid());
                        if (userdata.child("name").exists()) {
                            name[0] = userdata.child("name").getValue(String.class);
                        } else {
                            name[0] = null;
                        }

                        for (DataSnapshot snapshot1: snapshot.child("userdata").getChildren()) {
                            Util.key2name.put(snapshot1.getKey(), snapshot1.child("name").getValue(String.class));
                        }

                        for (DataSnapshot data: snapshot.child("projects").getChildren()) {
                            ArrayList<Commit> commits = new ArrayList<>();
                            ArrayList<String> collaborators = new ArrayList<>();

                            for (DataSnapshot collaborator: data.child("collaborators").getChildren()) {
                                collaborators.add(collaborator.getValue(String.class));
                            }

                            for (DataSnapshot commits_: data.child("commits").getChildren()) {
                                commits.add(new Commit(
                                        commits_.child("author").getValue(String.class),
                                        snapshot.child("userdata").child(commits_.child("author").getValue(String.class)).child("name").getValue(String.class),
                                        commits_.child("timestamp").getValue(int.class),
                                        commits_.child("changes").getValue(String.class)
                                ));
                            }

                            OnlineProject project = new OnlineProject(
                                    data.child("name").getValue(String.class),
                                    String.valueOf(data.child("version").getValue(double.class)),
                                    data.child("author").getValue(String.class),
                                    snapshot.child("userdata").child(data.child("author").getValue(String.class)).child("name").getValue(String.class),
                                    data.child("open").getValue(Boolean.class),
                                    false,
                                    data.getKey(),
                                    commits,
                                    collaborators);

                            if (Objects.equals(data.child("author").getValue(String.class), user.getUid())) {
                                onlineProjects.add(project);
                            }
                            allOnlineProjects.add(project);
                        }

                        for (DataSnapshot data: userdata.child("projects").getChildren()) {
                            ArrayList<Commit> commits = new ArrayList<>();
                            ArrayList<String> collaborators = new ArrayList<>();

                            for (DataSnapshot collaborator: data.child("collaborators").getChildren()) {
                                collaborators.add(collaborator.getValue(String.class));
                            }

                            for (DataSnapshot commits_: data.child("commits").getChildren()) {
                                commits.add(new Commit(
                                        commits_.child("author").getValue(String.class),
                                        snapshot.child("userdata").child(commits_.child("author").getValue(String.class)).child("name").getValue(String.class),
                                        commits_.child("timestamp").getValue(int.class),
                                        commits_.child("changes").getValue(String.class)
                                ));
                            }

                            onlineProjects.add(new OnlineProject(
                                    data.child("name").getValue(String.class),
                                    String.valueOf(data.child("version").getValue(double.class)),
                                    data.child("author").getValue(String.class),
                                    snapshot.child("userdata").child(data.child("author").getValue(String.class)).child("name").getValue(String.class),
                                    data.child("open").getValue(Boolean.class),
                                    false,
                                    data.getKey(),
                                    commits,
                                    collaborators));
                        }

                        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(HomeActivity.this, drawerLayout, R.string.open_drawer, R.string.close_drawer);
                        drawerLayout.addDrawerListener(toggle);
                        toggle.syncState();

                        if (savedInstanceState == null) {
                            getSupportFragmentManager().beginTransaction().replace(R.id.framelayout,
                                    new MainFragment(user, name[0], changeName, sketchwareProjects, nv, toolbar, onlineProjects)).commit();
                            findViewById(R.id.progressHome).setVisibility(View.GONE);
                            nv.setCheckedItem(R.id.drawer_home);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(findViewById(R.id.framelayout), error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                    }
                });

        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.drawer_home:
                        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout,
                                new MainFragment(user, name[0], changeName, sketchwareProjects, nv, toolbar, onlineProjects)).commit();
                        toolbar.setTitle("Home");
                        break;
                    case R.id.drawer_projects:
                        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout,
                                new ProjectsFragment(sketchwareProjects)).commit();
                        toolbar.setTitle("Projects");
                        break;
                    case R.id.drawer_online_proj:
                        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout,
                                new OnlineFragment(allOnlineProjects)).commit();
                        toolbar.setTitle("Online Projects");
                        break;
                    case R.id.drawer_collaborate:
                        Log.d(TAG, "onNavigationItemSelected: " + onlineProjects.toString());
                        getSupportFragmentManager().beginTransaction().replace(R.id.framelayout,
                                new CollaborateFragment(onlineProjects)).commit();
                        toolbar.setTitle("Collaborate");
                        break;
                    case R.id.drawer_share:
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                        sendIntent.setType("text/plain");
                        Intent shareIntent = Intent.createChooser(sendIntent, null);
                        startActivity(shareIntent);
                        break;
                    case R.id.drawer_upload:
                        Intent i = new Intent(HomeActivity.this, UploadActivity.class);
                        startActivity(i);
                        break;
                    default:
                        Toast.makeText(HomeActivity.this, "ERROR", Toast.LENGTH_LONG);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void changeName() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference db = database.getReference("userdata/".concat(auth.getCurrentUser().getUid()));
        final EditText edittext = new EditText(HomeActivity.this);
        AlertDialog.Builder alert = new AlertDialog.Builder(HomeActivity.this);
        alert.setTitle(getString(R.string.change_name));

        alert.setView(edittext);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String text = edittext.getText().toString().trim();
                if (text.equals("")) {
                    edittext.setError(getString(R.string.name_empty));
                } else if (!RegisterActivity.isAsciiPrintable(text)) {
                    edittext.setError(getString(R.string.name_not_ascii));
                } else {
                    db.child("name").setValue(text)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(HomeActivity.this, "Success, Refresh to see your new name!", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(HomeActivity.this, "ERROR: ".concat(e.getMessage()), Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                    dialog.dismiss();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void storagePerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 1000);
            } else {
                getSketchwareProjects();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            getSketchwareProjects();
        }
    }

    public ArrayList<String> listDir(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        File file = new File(str);
        if (file.exists() && !file.isFile()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                arrayList.clear();
                for (File absolutePath : listFiles) {
                    arrayList.add(absolutePath.getAbsolutePath());
                }
            }
        }
        return arrayList;
    }

    private void getSketchwareProjects() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/.sketchware/mysc/list/";
        Log.d(TAG, "getSketchwareProjects: " +path);
        for (String pat: listDir(path)) {
            try {
                Log.d(TAG, "getSketchwareProjects: " + pat + "/project");
                JSONObject project = new JSONObject(decrypt(pat + "/project"));
                SketchwareProject sw_proj = new SketchwareProject(
                        project.getString("my_app_name"),
                        project.getString("sc_ver_name"),
                        project.getString("my_sc_pkg_name"),
                        project.getString("my_ws_name"),
                        project.getString("sc_id"));
                Toast.makeText(this, project.getString("my_app_name"), Toast.LENGTH_SHORT);
                sketchwareProjects.add(0, sw_proj);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
                Log.e(TAG, "getSketchwareProjects: " + e.toString());
            }
        }
    }

    private String decrypt(String path) {
        try {
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");;
            byte[] bytes = "sketchwaresecure".getBytes();
            instance.init(2, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
            RandomAccessFile randomAccessFile = new RandomAccessFile(path, "r");
            byte[] bArr = new byte[((int) randomAccessFile.length())];
            randomAccessFile.readFully(bArr);
            return new String(instance.doFinal(bArr));
        } catch (Exception e) {
            Snackbar.make(findViewById(R.id.framelayout), e.getMessage(), Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.RED)
                    .show();
        }
        return "ERROR WHILE PARSING";
    }
}