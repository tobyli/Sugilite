package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.model.SetMapEntrySerializableWrapper;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.ui.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.ui.UIElementFeatureRecommender;

public class RecordingPopUpActivity extends AppCompatActivity {
    private SugiliteAvailableFeaturePack featurePack;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private Set<Map.Entry<String, String>> allParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allChildFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedChildFeatures = new HashSet<>();
    private SugiliteData sugiliteData;
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private UIElementFeatureRecommender recommender;
    static final int PICK_CHILD_FEATURE = 1;
    static final int PICK_PARENT_FEATURE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        sugiliteScriptDao = new SugiliteScriptDao(this);
        readableDescriptionGenerator = new ReadableDescriptionGenerator(getApplicationContext());
        setContentView(R.layout.activity_recoding_pop_up);
        featurePack = new SugiliteAvailableFeaturePack();
        //fetch the data capsuled in the intent
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras == null){
                //something wrong here!
            }
            else{
                featurePack.packageName = extras.getString("packageName", "NULL");
                featurePack.className = extras.getString("className", "NULL");
                featurePack.text = extras.getString("text", "NULL");
                featurePack.contentDescription = extras.getString("contentDescription", "NULL");
                featurePack.viewId = extras.getString("viewId", "NULL");
                featurePack.boundsInParent = extras.getString("boundsInParent", "NULL");
                featurePack.boundsInScreen = extras.getString("boundsInScreen", "NULL");
                featurePack.time = extras.getLong("time", -1);
                featurePack.eventType = extras.getInt("eventType", -1);
                featurePack.parentNode = extras.getParcelable("parentNode");
                featurePack.childNodes = extras.getParcelable("childrenNodes");
                featurePack.allNodes = extras.getParcelable("allNodes");
                featurePack.isEditable = extras.getBoolean("isEditable");
                featurePack.eventType = extras.getInt("eventType");
                featurePack.screenshot = (File)extras.getSerializable("screenshot");
            }
        }
        else{
            featurePack.packageName = savedInstanceState.getString("packageName", "NULL");
            featurePack.className = savedInstanceState.getString("className", "NULL");
            featurePack.text = savedInstanceState.getString("text", "NULL");
            featurePack.contentDescription = savedInstanceState.getString("contentDescription", "NULL");
            featurePack.viewId = savedInstanceState.getString("viewId", "NULL");
            featurePack.boundsInParent = savedInstanceState.getString("boundsInParent", "NULL");
            featurePack.boundsInScreen = savedInstanceState.getString("boundsInScreen", "NULL");
            featurePack.time = savedInstanceState.getLong("time", -1);
            featurePack.eventType = savedInstanceState.getInt("eventType", -1);
            featurePack.parentNode = savedInstanceState.getParcelable("parentNode");
            featurePack.childNodes = savedInstanceState.getParcelable("childrenNodes");
            featurePack.allNodes = savedInstanceState.getParcelable("allNodes");
            featurePack.isEditable = savedInstanceState.getBoolean("isEditable");
            featurePack.eventType = savedInstanceState.getInt("eventType");
            featurePack.screenshot = (File)savedInstanceState.getSerializable("screenshot");
        }

        //populate parent features
        if(featurePack.parentNode != null){
            if(featurePack.parentNode.getText() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("text", featurePack.parentNode.getText().toString()));
            if(featurePack.parentNode.getContentDescription() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("contentDescription", featurePack.parentNode.getContentDescription().toString()));
            if(featurePack.parentNode.getViewIdResourceName() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("viewId", featurePack.parentNode.getViewIdResourceName()));
        }
        if(allParentFeatures.size() == 0)
            ((ViewManager)findViewById(R.id.parentCheckbox).getParent()).removeView(findViewById(R.id.parentCheckbox));


        //populate child features
        for(AccessibilityNodeInfo childNode : featurePack.childNodes.getList()){
            if(childNode != null){
                if(childNode.getText() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("text", childNode.getText().toString()));
                if(childNode.getContentDescription() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("contentDescription", childNode.getContentDescription().toString()));
                if(childNode.getViewIdResourceName() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("viewId", childNode.getViewIdResourceName()));
            }
        }
        if(allChildFeatures.size() == 0)
            ((ViewManager)findViewById(R.id.childrenCheckbox).getParent()).removeView(findViewById(R.id.childrenCheckbox));

        recommender = new UIElementFeatureRecommender(featurePack.packageName, featurePack.className, featurePack.text, featurePack.contentDescription, featurePack.viewId, featurePack.boundsInParent, featurePack.boundsInScreen, featurePack.isEditable, featurePack.time, featurePack.eventType, allParentFeatures, allChildFeatures);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(featurePack.time);
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        boolean autoFillEnabled = sharedPreferences.getBoolean("auto_fill_enabled", false);

        ((CheckBox)findViewById(R.id.packageName)).setText("App Name: " + readableDescriptionGenerator.getReadableName(featurePack.packageName));
        //by default: check package name & class name
        ((CheckBox)findViewById(R.id.packageName)).setChecked(true);
        ((CheckBox)findViewById(R.id.className)).setText("Class Name: " + featurePack.className);
        ((CheckBox)findViewById(R.id.className)).setChecked(true);


        if(!featurePack.text.contentEquals("NULL")) {
            ((CheckBox) findViewById(R.id.text)).setText("Text: " + featurePack.text);
            if(autoFillEnabled)
                ((CheckBox) findViewById(R.id.text)).setChecked(recommender.chooseText());
        }
        else
            ((ViewManager)findViewById(R.id.text).getParent()).removeView(findViewById(R.id.text));

        if(!featurePack.contentDescription.contentEquals("NULL")) {
            ((CheckBox) findViewById(R.id.contentDescription)).setText("Content Description: " + featurePack.contentDescription);
            if(autoFillEnabled)
                ((CheckBox) findViewById(R.id.contentDescription)).setChecked(recommender.chooseContentDescription());
        }
        else
            ((ViewManager)findViewById(R.id.contentDescription).getParent()).removeView(findViewById(R.id.contentDescription));

        if(!featurePack.viewId.contentEquals("NULL")) {
            ((CheckBox) findViewById(R.id.viewId)).setText("ViewId: " + featurePack.viewId);
            if(autoFillEnabled)
                ((CheckBox) findViewById(R.id.viewId)).setChecked(recommender.chooseViewId());
        }
        else
            ((ViewManager)findViewById(R.id.viewId).getParent()).removeView(findViewById(R.id.viewId));


        ((CheckBox)findViewById(R.id.boundsInParent)).setText("Bounds in Parent: " + featurePack.boundsInParent);
        if(autoFillEnabled)
            ((CheckBox) findViewById(R.id.boundsInParent)).setChecked(recommender.chooseBoundsInParent());

        ((CheckBox)findViewById(R.id.boundsInScreen)).setText("Bounds in Screen: " + featurePack.boundsInScreen);
        if(autoFillEnabled)
            ((CheckBox) findViewById(R.id.boundsInScreen)).setChecked(recommender.chooseBoundsInScreen());

        //populate selected child/parent features
        if(autoFillEnabled){
            selectedChildFeatures.addAll(recommender.chooseChildFeatures());
            selectedParentFeatures.addAll(recommender.chooseParentFeatures());
            if(findViewById(R.id.childrenCheckbox) != null)
                ((CheckBox)findViewById(R.id.childrenCheckbox)).setChecked(selectedChildFeatures.size() > 0);
            if(findViewById(R.id.parentCheckbox) != null)
                ((CheckBox)findViewById(R.id.parentCheckbox)).setChecked(selectedParentFeatures.size() > 0);
        }


        ((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()) + "\nRecording script: " + sharedPreferences.getString("scriptName", "NULL"));
        ((TextView)findViewById(R.id.filteredNodeCount)).setText(generateFilterCount());
        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());

    }

    public void finishActivity(View view){

        finish();
    }
    public void turnOffRecording(View view)
    {

        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        finish();
    }

    public void OKButtonOnClick(View view){

        //add head if no one is present
        if(sugiliteData.getScriptHead() == null ||
                (!((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName().contentEquals(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"))){
            sugiliteData.setScriptHead(new SugiliteStartingBlock(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"));
            sugiliteData.setCurrentScriptBlock(sugiliteData.getScriptHead());
        }
        //use the dialog "builder" to ask the type of operation to take
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Operation");
        String[] operations = {};
        if(featurePack.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (featurePack.isEditable)
                operations = new String[]{"CLICK", "SET TEXT"};
            else
                operations = new String[]{"CLICK"};
            final SugiliteOperation sugiliteOperation = new SugiliteOperation();
            sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
            final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteOperation);
            final Context activityContext = this;
            builder.setItems(operations, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Toast.makeText(getApplicationContext(), "CLICK REQUESTED", Toast.LENGTH_SHORT).show();
                            sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
                            break;
                        case 1:
                            Toast.makeText(getApplicationContext(), "SET_TEXT REQUESTED", Toast.LENGTH_SHORT).show();
                            sugiliteOperation.setOperationType(SugiliteOperation.SET_TEXT);
                            break;
                    }
                    operationBlock.setDescription(generateDescription());
                    operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
                    operationBlock.setElementMatchingFilter(generateFilter());
                    operationBlock.setScreenshot(featurePack.screenshot);
                    //genereate the block if the operation is click or return
                    if (sugiliteOperation.getOperationType() == SugiliteOperation.CLICK || sugiliteOperation.getOperationType() == SugiliteOperation.RETURN) {
                        operationBlock.setOperation(sugiliteOperation);
                        if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
                            ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                        }
                        if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
                            ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                        }
                        String message = "";
                        if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK) {
                            message += "Click ";
                        }
                        if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT) {
                            message += "Set text to \"" + ((SugiliteSetTextOperation) operationBlock.getOperation()).getText() + "\" ";
                        }
                        message += generateDescription();
                        operationBlock.setDescription(readableDescriptionGenerator.generateReadableDescription(operationBlock));
                        sugiliteData.setCurrentScriptBlock(operationBlock);
                        try {
                            sugiliteScriptDao.save((SugiliteStartingBlock) sugiliteData.getScriptHead());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("saved block");
                        new AlertDialog.Builder(activityContext)
                                .setTitle("Operation Recorded")
                                .setMessage(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(operationBlock)))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                        finish();
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    // if the operation == text, add a new pop up to ask the text to set the content of the edittext widget to
                    //TODO: eliminate the duplicate code for the two branches
                    else if (sugiliteOperation.getOperationType() == SugiliteOperation.SET_TEXT) {
                        final SugiliteSetTextOperation setTextOperation = new SugiliteSetTextOperation();
                        AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(activityContext);
                        textDialogBuilder.setTitle("Set Text Operation").setMessage("Enter the text to set to");
                        final EditText editText = new EditText(activityContext);
                        textDialogBuilder.setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String text = editText.getText().toString();
                                setTextOperation.setText(text);
                                operationBlock.setOperation(setTextOperation);
                                if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
                                    ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                                }
                                if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
                                    ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                                }
                                System.out.println("saved block");
                                String message = "";
                                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK) {
                                    message += "Click ";
                                }
                                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT) {
                                    message += "Set text to \"" + ((SugiliteSetTextOperation) operationBlock.getOperation()).getText() + "\" ";
                                }
                                message += generateDescription();
                                operationBlock.setDescription(readableDescriptionGenerator.generateReadableDescription(operationBlock));
                                sugiliteData.setCurrentScriptBlock(operationBlock);
                                try {
                                    sugiliteScriptDao.save((SugiliteStartingBlock) sugiliteData.getScriptHead());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                new AlertDialog.Builder(activityContext)
                                        .setTitle("Operation Recorded")
                                        .setMessage(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(operationBlock)))
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                sugiliteData.addInstruction(operationBlock);
                                                // continue with delete
                                                finish();
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }
                        });
                        textDialogBuilder.show();

                    }
                }
            });
            AlertDialog dialog = builder.show();
        }


        else{
            String defaultOperationName = "NULL";
            String message = "";
            SugiliteOperation sugiliteOperation = new SugiliteOperation();
            switch (featurePack.eventType){
                case AccessibilityEvent.TYPE_VIEW_SELECTED:
                    defaultOperationName = "SELECT";
                    sugiliteOperation.setOperationType(SugiliteOperation.SELECT);
                    message += "Select ";
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    defaultOperationName = "LONG CLICK";
                    sugiliteOperation.setOperationType(SugiliteOperation.LONG_CLICK);
                    message += "Long click ";
                    break;
            }
            final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(sugiliteOperation);
            final Context activityContext = this;
            operationBlock.setDescription(generateDescription());
            operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
            operationBlock.setElementMatchingFilter(generateFilter());
            operationBlock.setOperation(sugiliteOperation);
            operationBlock.setScreenshot(featurePack.screenshot);


            if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
                ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
            }
            if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
                ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
            }
            message += generateDescription();
            operationBlock.setDescription(readableDescriptionGenerator.generateReadableDescription(operationBlock));
            sugiliteData.setCurrentScriptBlock(operationBlock);
            try {
                sugiliteScriptDao.save((SugiliteStartingBlock) sugiliteData.getScriptHead());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("saved block");
            new AlertDialog.Builder(activityContext)
                    .setTitle("Operation Recorded")
                    .setMessage(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(operationBlock)))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    public void entryOnSelect(View view){

        int viewId = view.getId();

        if(viewId == R.id.packageName){
            Toast.makeText(this, "Selected Package Name as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.className){
            Toast.makeText(this, "Selected Class Name as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.text){
            Toast.makeText(this, "Selected Text as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.contentDescription){
            Toast.makeText(this, "Selected Content Description as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.viewId){
            Toast.makeText(this, "Selected View ID as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.boundsInParent){
            Toast.makeText(this, "Selected Bound in Parent as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.boundsInScreen){
            Toast.makeText(this, "Selected Bount in Screen as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.childrenCheckbox){
            Toast.makeText(this, "Selected Children as Identifying Feature", Toast.LENGTH_SHORT).show();
            Intent popUpSubMenuIntent = new Intent(this, RecordingPopupSubMenuActivity.class);
            popUpSubMenuIntent.putExtra("allFeatures", new SetMapEntrySerializableWrapper(allChildFeatures));
            popUpSubMenuIntent.putExtra("selectedFeatures", new SetMapEntrySerializableWrapper(selectedChildFeatures));
            startActivityForResult(popUpSubMenuIntent, PICK_CHILD_FEATURE);

        }
        if(viewId == R.id.parentCheckbox){
            Toast.makeText(this, "Selected Parent as Identifying Feature", Toast.LENGTH_SHORT).show();
            Intent popUpSubMenuIntent = new Intent(this, RecordingPopupSubMenuActivity.class);
            popUpSubMenuIntent.putExtra("allFeatures", new SetMapEntrySerializableWrapper(allParentFeatures));
            popUpSubMenuIntent.putExtra("selectedFeatures", new SetMapEntrySerializableWrapper(selectedParentFeatures));
            startActivityForResult(popUpSubMenuIntent, PICK_PARENT_FEATURE);

        }
        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());
        ((TextView)findViewById(R.id.filteredNodeCount)).setText(generateFilterCount());
    }

    //read and load the result from the child/parent sub activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_CHILD_FEATURE){
            //get result from child feature picking
            if(resultCode == RESULT_OK){
                selectedChildFeatures = ((SetMapEntrySerializableWrapper)data.getSerializableExtra("result")).set;
            }
            else{

            }
        }
        else if(requestCode == PICK_PARENT_FEATURE){
            //get result from parent feature picking
            if(resultCode == RESULT_OK){
                selectedParentFeatures = ((SetMapEntrySerializableWrapper)data.getSerializableExtra("result")).set;
            }
            else{

            }
        }

        //NOTE: children/parent checkbox may have been removed if the current node has no child/parent label
        if(findViewById(R.id.childrenCheckbox) != null)
            ((CheckBox)findViewById(R.id.childrenCheckbox)).setChecked(selectedChildFeatures.size() > 0);
        if(findViewById(R.id.parentCheckbox) != null)
            ((CheckBox)findViewById(R.id.parentCheckbox)).setChecked(selectedParentFeatures.size() > 0);


        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());
        ((TextView)findViewById(R.id.filteredNodeCount)).setText(generateFilterCount());
    }
    //show the parent popup when the parent checkbox is checked
    public void showParentPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        for(Map.Entry<String, String> entry : allParentFeatures){
            popup.getMenu().add(entry.getKey() + " is " + entry.getValue()).setCheckable(true).setChecked(false);
        }
        popup.show();
    }
    //show the children popup when the children checkbox is checked
    public void showChildrenPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        for(Map.Entry<String, String> entry : allChildFeatures){
            popup.getMenu().add(entry.getKey() + " is " + entry.getValue()).setCheckable(true).setChecked(false);
        }
        popup.show();
    }

    /**
     * generate a UIElementMatchingFilter based on the selection
     * @return
     */
    public UIElementMatchingFilter generateFilter(){
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        if((findViewById(R.id.packageName) != null) && (((CheckBox)findViewById(R.id.packageName)).isChecked())){
            filter.setPackageName(featurePack.packageName);
        }
        if((findViewById(R.id.className) != null) && ((CheckBox)findViewById(R.id.className)).isChecked()){
            filter.setClassName(featurePack.className);
        }
        if((findViewById(R.id.text) != null) && ((CheckBox)findViewById(R.id.text)).isChecked()){
            filter.setText(featurePack.text);
        }
        if((findViewById(R.id.contentDescription) != null) && ((CheckBox)findViewById(R.id.contentDescription)).isChecked()){
            filter.setContentDescription(featurePack.contentDescription);
        }
        if((findViewById(R.id.viewId) != null) && ((CheckBox)findViewById(R.id.viewId)).isChecked()){
            filter.setViewId(featurePack.viewId);
        }
        if((findViewById(R.id.boundsInParent) != null) && ((CheckBox)findViewById(R.id.boundsInParent)).isChecked()){
            filter.setBoundsInParent(Rect.unflattenFromString(featurePack.boundsInParent));
        }
        if((findViewById(R.id.boundsInScreen) != null) && ((CheckBox)findViewById(R.id.boundsInScreen)).isChecked()){
            filter.setBoundsInScreen(Rect.unflattenFromString(featurePack.boundsInScreen));
        }
        if (selectedChildFeatures.size() > 0){
            UIElementMatchingFilter childFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedChildFeatures){
                if(entry.getKey().contentEquals("text")){
                    childFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("contentDescription")){
                    childFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("viewId")){
                    childFilter.setViewId(entry.getValue());
                }
            }
            filter.setChildFilter(childFilter);
        }
        if (selectedParentFeatures.size() > 0){
            UIElementMatchingFilter parentFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedParentFeatures){
                if(entry.getKey().contentEquals("text")){
                    parentFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("contentDescription")){
                    parentFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("viewId")){
                    parentFilter.setViewId(entry.getValue());
                }
            }
            filter.setParentFilter(parentFilter);
        }
        return filter;
    }

    public String generateFilterCount(){
        UIElementMatchingFilter filter = generateFilter();
        List<AccessibilityNodeInfo> nodes = featurePack.allNodes.getList();
        int count = 0, clickableCount = 0;
        for(AccessibilityNodeInfo node : nodes){
            if(filter.filter(node)){
                count++;
                if(node.isClickable())
                    clickableCount++;
            }
        }
        return "Filtered " + count + " elements on current screen, " + clickableCount + " clickable out of " + nodes.size();
    }

    /**
     * generate a description string based on the selection
     * @return a description string
     */
    public String generateDescription(){
        boolean notFirstCondition = false;
        String retVal = "";
        /*
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED){
            retVal += "Click ";
        }
        */
        retVal += "on UI element that ";
        if((findViewById(R.id.packageName) != null) && ((CheckBox)findViewById(R.id.packageName)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "is within the package \"" + featurePack.packageName + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.className) != null) && ((CheckBox)findViewById(R.id.className)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "is of the class type \"" + featurePack.className + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.text) != null) && ((CheckBox)findViewById(R.id.text)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has text \"" + featurePack.text + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.contentDescription) != null) && ((CheckBox)findViewById(R.id.contentDescription)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has contentDescription \"" + featurePack.contentDescription + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.viewId) != null) && ((CheckBox)findViewById(R.id.viewId)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has view ID \"" + featurePack.viewId + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.boundsInParent) != null) && ((CheckBox)findViewById(R.id.boundsInParent)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has location relative to its parent element at \"" + featurePack.boundsInParent + "\" ");
            notFirstCondition = true;
        }
        if((findViewById(R.id.boundsInScreen) != null) && ((CheckBox)findViewById(R.id.boundsInScreen)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has location on screen at \"" + featurePack.boundsInScreen + "\" ");
            notFirstCondition = true;
        }
        if (selectedChildFeatures.size() > 0){
            retVal += ((notFirstCondition? "and " : "") + "has a child that { ");
            boolean notFirst = false;
            for(Map.Entry<String, String> entry :selectedChildFeatures){
                retVal += ((notFirst? "and " : "") + "has " + entry.getKey() + " == \"" + entry.getValue() + "\" ");
                notFirst = true;
            }
            retVal += "} ";
        }
        if (selectedParentFeatures.size() > 0){
            retVal += ((notFirstCondition? "and " : "") + "has the parent that { ");
            boolean notFirst = false;
            for(Map.Entry<String, String> entry :selectedParentFeatures){
                retVal += ((notFirst? "and " : "") + "has " + entry.getKey() + " == \"" + entry.getValue() + "\" ");
                notFirst = true;
            }
            retVal += "} ";
        }
        if(notFirstCondition)
            return retVal;
        else
            return "No feature selected!";
    }


}
