package fr.natinusala.openedt.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import fr.natinusala.openedt.R;
import fr.natinusala.openedt.data.Group;
import fr.natinusala.openedt.data.Week;
import fr.natinusala.openedt.manager.DataManager;
import fr.natinusala.openedt.manager.GroupManager;
import fr.natinusala.openedt.utils.TimeUtils;
import fr.natinusala.openedt.view.WeekView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static String INTENT_SELECT_LAST_ONE = "slo";

    public static int TABS_COUNT = 3;

    //TODO Bouton refresh / réessayer

    NavigationView navigationView;
    ArrayList<Group> groups;
    MainActivity instance = this;
    Group selectedGroup;
    ViewPager viewPager;
    DrawerLayout drawer;
    ProgressBar progressBar;

    ArrayList<Week> weeks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle("OpenEDT");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.main_root);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        viewPager = (ViewPager) findViewById(R.id.main_pager);
        viewPager.setOffscreenPageLimit(TABS_COUNT - 1);
        progressBar = (ProgressBar) this.findViewById(R.id.main_progressBar);

        //Refresh
        refresh(true);
    }

    public static int DRAWER_GROUP_ID = 42;

    public void refresh(boolean loadFromFile) {
        //Chargement de la liste des groupes
        if (loadFromFile) {
            groups = new ArrayList<>(Arrays.asList(GroupManager.readGroups(this)));
        }

        if (groups.isEmpty()) {
            this.startActivity(new Intent(this, AddGroupActivity.class));
            finish();
            return;
        }

        SubMenu subMenu = navigationView.getMenu().getItem(0).getSubMenu();
        subMenu.clear();

        for (int i = 0; i < groups.size(); i++) {
            Group g = groups.get(i);
            subMenu.add(DRAWER_GROUP_ID, i, Menu.FIRST, g.name);
        }
        subMenu.setGroupCheckable(DRAWER_GROUP_ID, true, true);

        navigationView.invalidate();

        Group lastSelectedGroup = GroupManager.getSelectedGroup(this);
        //Si on a un SELECT_LAST_ONE de défini
        if (getIntent().getBooleanExtra(INTENT_SELECT_LAST_ONE, false))
        {
            selectGroup(groups.get(groups.size()-1));
        }
        else if (lastSelectedGroup != null && groups.contains(lastSelectedGroup))
        {
            selectGroup(lastSelectedGroup);
        }
        else
        {
            selectGroup(groups.get(0));
        }
    }

    void selectGroup(Group g)
    {
        if (selectedGroup != null && groups.contains(selectedGroup))
        {
            navigationView.getMenu().getItem(0).getSubMenu().findItem(groups.indexOf(selectedGroup)).setChecked(false);
        }
        navigationView.getMenu().getItem(0).getSubMenu().findItem(groups.indexOf(g)).setChecked(true);
        selectedGroup = g;
        GroupManager.saveSelectedGroup(this, g);
        loadSelectedGroup();
    }

    void loadSelectedGroup()
    {
        this.setTitle(selectedGroup.name);

        //Chargement de la liste des semaines
        new Task().execute();
    }

    //TODO Spinner

    class Task extends AsyncTask<Void, Void, Boolean>
    {
        TabsAdapter adapter;

        @Override
        protected void onPreExecute()
        {
            viewPager.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            Week[] data = DataManager.getWeeksForGroup(instance, selectedGroup);

            if (data != null)
            {
                weeks = new ArrayList<>(Arrays.asList(data));
                adapter = new TabsAdapter(getSupportFragmentManager());
                return true;
            }
            else
            {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            if (result)
            {
                //Affichage des données
                viewPager.setAdapter(adapter);

                viewPager.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }
            else
            {
                final Snackbar snackbar = Snackbar.make(findViewById(R.id.main_root), "Impossible de charger les données.", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Réessayer", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Task().execute();
                        snackbar.dismiss();
                    }
                });
                snackbar.show();
            }

        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_group)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Attention !");
            dialog.setMessage("Etes-vous sûr de vouloir supprimer ce groupe ?");
            dialog.setNegativeButton("Non", null);
            dialog.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    groups.remove(selectedGroup);
                    GroupManager.saveGroups(instance, groups.toArray(new Group[groups.size()]));
                    refresh(false);
                }
            });
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        if (item.getGroupId() == DRAWER_GROUP_ID)
        {
            selectGroup(groups.get(item.getItemId()));
        }
        else if (item.getItemId() == R.id.add_group)
        {
            this.startActivity(new Intent(this, AddGroupActivity.class));
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    class TabsAdapter extends FragmentPagerAdapter
    {
        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case 0:
                    return new HomeFragment();
                case 1:
                    return new DaysFragment();
                case 2:
                    return new WeeksFragment();
            }

            return null;
        }

        @Override
        public int getCount() {
            return TABS_COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "ACCUEIL";
                case 1:
                    return "JOURS";
                case 2:
                    return "SEMAINES";
            }
            return null;
        }
    }

    class HomeFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.activity_main_home_fragment, container, false);
        }
    }

    class DaysFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.activity_main_days_fragment, container, false);
        }
    }

    class WeeksFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            View root = inflater.inflate(R.layout.activity_main_weeks_fragment, container, false);

            LinearLayout weeksContainer = (LinearLayout) root.findViewById(R.id.weeks_container);

            Calendar cal = Calendar.getInstance();
            int weekCal = cal.get(Calendar.WEEK_OF_YEAR) + 1;
            int week = TimeUtils.getIdWeek(weekCal);

            weeksContainer.addView(new WeekView(instance, weeks.get(week-1)));
            weeksContainer.addView(new WeekView(instance, weeks.get(week)));
            weeksContainer.addView(new WeekView(instance, weeks.get(week+1)));

            return root;
        }
    }
}
