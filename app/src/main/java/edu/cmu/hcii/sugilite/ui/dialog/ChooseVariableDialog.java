package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 7/15/16
 * @time 3:20 PM
 */
public class ChooseVariableDialog implements AbstractSugiliteDialog {
    private Context context;
    private AlertDialog dialog;
    private String selectedItemName;
    private SugiliteData sugiliteData;
    private SugiliteStartingBlock startingBlock;
    private String defaultDefaultValue;
    private final EditText newVariableNameEditText;
    private final EditText defaultValueEditText;
    private final TextView editText;
    private final String label;

    public ChooseVariableDialog(final Context context, final TextView editText, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock, String label, String defaultDefaultValue){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_choose_variable, null);
        List<String> existingVariables = new ArrayList<>();
        for(Map.Entry<String, VariableValue> entry : startingBlock.variableNameDefaultValueMap.entrySet()){
            if(entry.getValue().getVariableValue() instanceof String){
                existingVariables.add(entry.getKey() + ": (" + (String) entry.getValue().getVariableValue() + ")");
            }
        }
        final ListView variableList = (ListView)dialogView.findViewById(R.id.existing_variable_list);
        if(variableList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, existingVariables);
            variableList.setAdapter(adapter);
        }
        View emptyView = dialogView.findViewById(R.id.empty);
        variableList.setEmptyView(emptyView);

        newVariableNameEditText = (EditText)dialogView.findViewById(R.id.new_variable_name);
        defaultValueEditText = (EditText)dialogView.findViewById(R.id.variable_default_value);
        this.startingBlock = startingBlock;
        this.editText = editText;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.label = label;
        this.defaultDefaultValue = defaultDefaultValue;
        variableList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view instanceof TextView){
                    String entry = ((TextView) view).getText().toString();
                    selectedItemName = entry.substring(0, entry.indexOf(":"));
                    newVariableNameEditText.setText("");
                    defaultValueEditText.setText("");
                }
            }
        });


        newVariableNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    //reset the listview selection
                    selectedItemName = s.toString();
                    variableList.clearChoices();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        builder.setView(dialogView)
                .setTitle(Const.appNameUpperCase + " Variable Selection")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        dialog = builder.create();
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }
     public void show(){

         dialog.show();
         dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
         {
             @Override
             public void onClick(View v)
             {
                 String defaultValueToShow = "";
                 if (selectedItemName == null || selectedItemName.length() < 1) {
                     PumiceDemonstrationUtil.showSugiliteToast("No item selected!", Toast.LENGTH_SHORT);
                 } else if (newVariableNameEditText.getText().length() > 0 && defaultValueEditText.getText().length() < 1) {
                     PumiceDemonstrationUtil.showSugiliteToast("No default value provided", Toast.LENGTH_SHORT);
                 } else {
                     if(newVariableNameEditText.getText().length() > 0){
                         //add the new variable and the new default value to the symbol table
                         String variableName = newVariableNameEditText.getText().toString();
                         String defaultValue = defaultValueEditText.getText().toString();
                         defaultValueToShow = defaultValue;
                         if(sugiliteData.variableNameVariableValueMap == null) {
                             sugiliteData.variableNameVariableValueMap = new HashMap<String, VariableValue>();
                         }
                         sugiliteData.variableNameVariableValueMap.put(variableName, new VariableValue(variableName, defaultValue));
                         startingBlock.variableNameDefaultValueMap.put(variableName, new VariableValue(variableName, defaultValue));
                     }
                     else {
                         //TODO: user has selected an existing variable
                         VariableValue defaultVariableValue = startingBlock.variableNameDefaultValueMap.get(selectedItemName);
                         if(defaultVariableValue != null && defaultVariableValue.getVariableValue() instanceof String) {
                             defaultValueToShow = (String) defaultVariableValue.getVariableValue();
                         }
                     }
                     if(label.length() > 0){
                         //choosing variable for a generated checkbox row
                         editText.setText(Html.fromHtml("<b>" + label + ":</b> " + "[" + selectedItemName + "]" + ": (" + defaultValueToShow + ")"));
                     }
                     else {
                         editText.setText("[" + selectedItemName + "]" + ": (" + defaultValueToShow + ")");
                     }
                     dialog.dismiss();
                 }
             }
         });
         defaultValueEditText.setText(defaultDefaultValue);
         newVariableNameEditText.setText("");


     }
}
