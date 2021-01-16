package com.iyxan23.sketch.collab;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iyxan23.sketch.collab.adapters.SketchwareProjectAdapter;
import com.iyxan23.sketch.collab.models.SketchwareProject;

import java.util.ArrayList;

public class SketchwareProjectsListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketchware_projects_list);

        // Get the recyclerview
        RecyclerView projecs_list = findViewById(R.id.sketchware_project_list);
        projecs_list.setLayoutManager(new LinearLayoutManager(this));

        // Get and set the adapter
        SketchwareProjectAdapter adapter = new SketchwareProjectAdapter(this);
        projecs_list.setAdapter(adapter);

        // Restore the UI state if the sketchware projects are already fetched
        if (savedInstanceState != null) {
            if (!savedInstanceState.isEmpty()) {
                adapter.updateView(savedInstanceState.getParcelableArrayList("sketchware_projects"));
                return;
            }
        }

        // Load projects in a different thread
        new Thread(() -> {
            // Fetch projects
            ArrayList<SketchwareProject> projects = Util.fetch_sketchware_projects();

            if (savedInstanceState != null) savedInstanceState.putParcelableArrayList("sketchware_projects", projects);

            // Hide the progressbar
            findViewById(R.id.progressBar_swplist).setVisibility(View.GONE);

            // Update the data
            adapter.updateView(projects);
        }).start();
    }
}
