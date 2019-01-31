package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceDemonstrationUtil;

/**
 * @author toby
 * @date 12/19/18
 * @time 2:34 PM
 */

/**
 * the intent handler used for confirming whether the user wants to execute the script
 */
public class PumiceScriptExecutingConfirmationIntentHandler implements PumiceUtteranceIntentHandler {
    private SugiliteStartingBlock script;
    private Activity context;


    public PumiceScriptExecutingConfirmationIntentHandler(Activity context, SugiliteStartingBlock script){
        this.context = context;
        this.script = script;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.EXECUTION_POSITIVE)) {
            dialogManager.sendAgentMessage("Executing the script...", true, false);
            ServiceStatusManager serviceStatusManager = dialogManager.getServiceStatusManager();
            SugiliteData sugiliteData = dialogManager.getSugiliteData();
            SharedPreferences sharedPreferences = dialogManager.getSharedPreferences();

            //load the knowledge manager into SugiliteData


            //===execute the script
            new AlertDialog.Builder(context)
                    .setTitle("Run Script")
                    .setMessage("Are you sure you want to run this script?")
                    .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //execute the script
                            PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, context.getLayoutInflater(), sharedPreferences, dialogManager, null, null);

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();


            //go back to the default intent handler
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context));

        } else {
            dialogManager.sendAgentMessage("OK", true, false);
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context));
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.EXECUTION_POSITIVE;
        } else {
            return PumiceIntent.EXECUTION_NEGATIVE;
        }
    }

}
