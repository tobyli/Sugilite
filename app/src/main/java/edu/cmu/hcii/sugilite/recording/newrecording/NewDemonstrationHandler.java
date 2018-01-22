package edu.cmu.hcii.sugilite.recording.newrecording;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

/**
 * @author toby
 * @date 1/5/18
 * @time 4:59 PM
 */
public class NewDemonstrationHandler {
    private SugiliteData sugiliteData;
    private Context context;
    private SharedPreferences sharedPreferences;
    private LayoutInflater layoutInflater;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private ReadableDescriptionGenerator readableDescriptionGenerator;

    public NewDemonstrationHandler(SugiliteData sugiliteData, Context context, LayoutInflater layoutInflater, SharedPreferences sharedPreferences){
        this.sugiliteData = sugiliteData;
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.layoutInflater = layoutInflater;
        this.blockBuildingHelper = new SugiliteBlockBuildingHelper(context, sugiliteData);
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
    }

    //handles the demonstration
    public void handleEvent(SugiliteAvailableFeaturePack featurePack, Set<Map.Entry<String, String>> availableAlternatives, UISnapshot uiSnapshot){
        //determine if disambiguation is needed

        //show the confirmation popup if not ambiguous
        List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList = blockBuildingHelper.generateDefaultQueries(featurePack, uiSnapshot);
        if(queryScoreList.size() > 0) {
            if (queryScoreList.size() <= 1 || (queryScoreList.get(1).getValue().intValue() - queryScoreList.get(0).getValue().intValue() >= 1)) {
                //not ambiguous, show the confirmation popup
                SugiliteOperationBlock block = blockBuildingHelper.getOperationFromQuery(queryScoreList.get(0).getKey(), SugiliteOperation.CLICK, featurePack);
                showConfirmation(block, featurePack, queryScoreList);
            }
            else{
                //ask for clarification if ambiguous
                Toast.makeText(context, "Ambiguous!", Toast.LENGTH_SHORT).show();
                showAmbiguousPopup(queryScoreList, featurePack);
            }
        }
        else{
            //empty result
            Toast.makeText(context, "Empty Result!", Toast.LENGTH_SHORT).show();
        }

    }

    private boolean isAmbiguous(SugiliteAvailableFeaturePack featurePack){

        return false;
    }

    private void showAmbiguousPopup(List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList, SugiliteAvailableFeaturePack featurePack){
        //the temporary popup to show for when the demonstration is ambiguous
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select from disambiguation results");
        ListView mainListView = new ListView(context);
        Map<TextView, SugiliteOperationBlock> textViews = new HashMap<>();
        String[] stringArray = new String[queryScoreList.size()];
        SugiliteOperationBlock[] sugiliteOperationBlockArray = new SugiliteOperationBlock[queryScoreList.size()];

        int i = 0;
        for(Map.Entry<SerializableOntologyQuery, Double> entry : queryScoreList){
            SugiliteOperationBlock block = blockBuildingHelper.getOperationFromQuery(entry.getKey(), SugiliteOperation.CLICK, featurePack);
            sugiliteOperationBlockArray[i++] = block;
        }

        Map<SugiliteOperationBlock, String> descriptions = blockBuildingHelper.getDescriptionsInDifferences(sugiliteOperationBlockArray);
        i = 0;
        for(SugiliteOperationBlock block : sugiliteOperationBlockArray){
            stringArray[i++] = descriptions.get(block);
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, stringArray)
        {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View row;
                if (null == convertView) {
                    row = layoutInflater.inflate(android.R.layout.simple_list_item_1, null);
                } else {
                    row = convertView;
                }
                TextView tv = (TextView) row.findViewById(android.R.id.text1);
                tv.setText(Html.fromHtml(getItem(position)));
                textViews.put(tv, sugiliteOperationBlockArray[position]);
                return row;
            }

        };
        mainListView.setAdapter(adapter);
        builder.setView(mainListView);
        AlertDialog dialog = builder.create();
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);
                blockBuildingHelper.saveBlock(sugiliteOperationBlockArray[position], featurePack);
                dialog.dismiss();
            }
        });
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    private void showConfirmation(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String newDescription = readableDescriptionGenerator.generateDescriptionForVerbalBlock(block, blockBuildingHelper.stripSerializableOntologyQuery(block.getQuery()).toString(), "UTTERANCE");
        builder.setTitle("Save Operation Confirmation").setMessage(Html.fromHtml("Are you sure you want to record the operation: " + newDescription));
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //save the block
                        blockBuildingHelper.saveBlock(block, featurePack);
                    }
                })
                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showAmbiguousPopup(queryScoreList, featurePack);
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}