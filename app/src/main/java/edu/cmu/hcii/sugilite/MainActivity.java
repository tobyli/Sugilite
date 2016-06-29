package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

public class MainActivity extends AppCompatActivity {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        View addButton = findViewById(R.id.addButton);
        serviceStatusManager = new ServiceStatusManager(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteScriptDao = new SugiliteScriptDao(this);
        sugiliteData = (SugiliteData)getApplication();
        //TODO: confirm overwrite when duplicated name
        //TODO: combine the two instances of script creation
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                sugiliteData.clearInstructionQueue();
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                final EditText scriptName = new EditText(v.getContext());
                scriptName.setText("New Script");
                scriptName.setSelectAllOnFocus(true);
                builder.setMessage("Specify the name for your new script")
                        .setView(scriptName)
                        .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!serviceStatusManager.isRunning()){
                                    //prompt the user if the accessiblity service is not active
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(v.getContext());
                                    builder1.setTitle("Service not running")
                                            .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    serviceStatusManager.promptEnabling();
                                                    //do nothing
                                                }
                                            }).show();
                                }
                                else if (scriptName != null && scriptName.getText().toString().length() > 0) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("scriptName", scriptName.getText().toString());
                                    editor.putBoolean("recording_in_process", true);
                                    editor.commit();
                                    //set the active script to the newly created script
                                    sugiliteData.initiateScript(scriptName.getText().toString() + ".SugiliteScript");
                                    //save the newly created script to DB
                                    try {
                                        sugiliteScriptDao.save((SugiliteStartingBlock)sugiliteData.getScriptHead());
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                                    setUpScriptList();
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing
                            }
                        })
                        .setTitle("New Script");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        setSupportActionBar(toolbar);
        setUpScriptList();
    }

    /**
     * update the script list displayed at the main activity according to the DB
     */
    private void setUpScriptList(){
        final ListView scriptList = (ListView)findViewById(R.id.scriptList);
        List<String> names = sugiliteScriptDao.getAllNames();
        scriptList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));
        final Context activityContext = this;
        scriptList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String scriptName = (String) scriptList.getItemAtPosition(position);
                final Intent scriptDetailIntent = new Intent(activityContext, ScriptDetailActivity.class);
                scriptDetailIntent.putExtra("scriptName", scriptName);
                startActivity(scriptDetailIntent);
            }
        });
        registerForContextMenu(scriptList);
    }

    private static final int ITEM_1 = Menu.FIRST;
    private static final int ITEM_2 = Menu.FIRST + 1;
    private static final int ITEM_3 = Menu.FIRST + 2;
    private static final int ITEM_4 = Menu.FIRST + 3;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info){
        super.onCreateContextMenu(menu, view, info);
        if(view instanceof TextView && ((TextView) view).getText() != null)
            menu.setHeaderTitle(((TextView) view).getText());
        else
            menu.setHeaderTitle("Sugilite Operation Menu");
        menu.add(0, ITEM_1, 0, "View");
        menu.add(0, ITEM_2, 0, "Rename");
        menu.add(0, ITEM_3, 0, "Share");
        menu.add(0, ITEM_4, 0, "Delete");
    }

    //TODO:implement context menu
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()){
        case ITEM_1:
            Toast.makeText(this, "View Script", Toast.LENGTH_SHORT).show();
            info.targetView.performClick();
            break;
        case ITEM_2:
            Toast.makeText(this, "Rename Script", Toast.LENGTH_SHORT).show();
            break;
        case ITEM_3:
            Toast.makeText(this, "Share Script", Toast.LENGTH_SHORT).show();
            break;
        case ITEM_4:
            Toast.makeText(this, "Delete Script", Toast.LENGTH_SHORT).show();
            if(info.targetView instanceof TextView && ((TextView) info.targetView).getText() != null) {
                sugiliteScriptDao.delete(((TextView) info.targetView).getText().toString());
                setUpScriptList();
            }
            break;
    }
    return super.onContextItemSelected(item);
    }

        @Override
    public void onResume(){
        super.onResume();
        setUpScriptList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.clear_automation_queue) {
            int count = sugiliteData.getInstructionQueueSize();
            sugiliteData.clearInstructionQueue();
            new AlertDialog.Builder(this)
                    .setTitle("Automation Queue Cleared")
                    .setMessage("Cleared " + count + " operations from the automation queue")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }

        if (id == R.id.clear_script_list) {
            int count = (int)sugiliteScriptDao.size();
            sugiliteScriptDao.clear();
            new AlertDialog.Builder(this)
                    .setTitle("Script List Cleared")
                    .setMessage("Cleared " + count + " scripts")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            setUpScriptList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
