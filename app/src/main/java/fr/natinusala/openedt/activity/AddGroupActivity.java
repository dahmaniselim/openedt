/*
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.natinusala.openedt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.natinusala.openedt.R;
import fr.natinusala.openedt.data.Component;
import fr.natinusala.openedt.data.Group;
import fr.natinusala.openedt.manager.GroupManager;

public class AddGroupActivity extends AppCompatActivity
{
    @Bind(R.id.componentSpinner) Spinner componentSpinner;
    @Bind(R.id.groupSpinner) Spinner groupSpinner;
    @Bind(R.id.groupCard) CardView groupCard;
    @Bind(R.id.addGroupButton) Button valider;
    @Bind(R.id.add_group_search_button) ImageButton searchButton;
    @Bind(R.id.groupTextView) AutoCompleteTextView groupTextView;

    ArrayAdapter<String> groupSpinnerAdapter;
    ArrayAdapter<String> groupTextViewAdapter;

    ArrayList<Group> groups;

    ArrayList<Group> addedGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.setTitle("Ajouter un groupe");

        //GroupSpinner
        groupSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        //Composante
        componentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                new Task().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                new Task().execute();
            }
        });
        ArrayAdapter<String> componentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);

        for (Component c : Component.values())
        {
            componentAdapter.add(c.name);
        }

        componentSpinner.setAdapter(componentAdapter);

        addedGroups = GroupManager.readGroups(this);

        //Bouton
        valider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Group selectedGroup = groups.get(groupSpinner.getSelectedItemPosition());
                if (addedGroups.contains(selectedGroup))
                {
                    final Snackbar snack = Snackbar.make(findViewById(R.id.add_group_root), "Ce groupe a déjà été ajouté.", Snackbar.LENGTH_LONG);
                    snack.show();
                }
                else
                {
                    GroupManager.addGroup(AddGroupActivity.this, selectedGroup);
                    Intent intent = new Intent(AddGroupActivity.this, MainActivity.class);
                    intent.putExtra(MainActivity.INTENT_SELECT_LAST_ONE, true);
                    startActivity(intent);
                    finish();
                }

            }
        });

        groupTextViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        groupTextView.setAdapter(groupTextViewAdapter);
        groupTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                groupSpinner.setSelection(groupSpinnerAdapter.getPosition(groupTextView.getText().toString()));
                showSpinner();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (groupSpinner.getVisibility() == View.VISIBLE) {
                    valider.setEnabled(false);
                    searchButton.setImageResource(R.drawable.ic_close_black_48dp);
                    groupSpinner.setVisibility(View.INVISIBLE);
                    groupTextView.setVisibility(View.VISIBLE);
                    toggleKeyboard(true);
                } else {
                    showSpinner();
                }
            }
        });

        new Task().execute();
    }

    void toggleKeyboard(boolean show)
    {
        View view = this.getCurrentFocus();
        if (view != null)
        {
            if (show)
            {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(groupTextView, 0);
            }
            else
            {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

        }
    }

    void showSpinner()
    {
        toggleKeyboard(false);
        valider.setEnabled(true);
        searchButton.setImageResource(R.drawable.ic_search_black_48dp);
        groupSpinner.setVisibility(View.VISIBLE);
        groupTextView.setVisibility(View.INVISIBLE);
    }

    class Task extends AsyncTask<Void, Void, Boolean>
    {
        Component selectedComponent;

        @Override
        protected void onPreExecute()
        {
            groupCard.setVisibility(View.INVISIBLE);
            valider.setVisibility(View.INVISIBLE);
            selectedComponent = Component.values()[componentSpinner.getSelectedItemPosition()];
            showSpinner();
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            try
            {
                groups = selectedComponent.sourceType.adapter.getGroupsList(selectedComponent);
                return true;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (result)
            {
                groupTextViewAdapter.clear();
                groupSpinnerAdapter.clear();
                for (Group g : groups)
                {
                    groupTextViewAdapter.add(g.name);
                    groupSpinnerAdapter.add(g.name);
                }

                groupCard.setVisibility(View.VISIBLE);
                valider.setVisibility(View.VISIBLE);
            }
            else
            {
                final Snackbar snack = Snackbar.make(findViewById(R.id.add_group_root), "Une erreur est survenue lors de la récupération des données.", Snackbar.LENGTH_INDEFINITE);
                snack.setAction("Réessayer", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Task().execute();
                        snack.dismiss();
                    }
                });
                snack.show();
            }
        }
    }

}
