package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.else_statement;

import android.app.Activity;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;

/**
 * @author toby
 * @date 3/11/19
 * @time 12:27 PM
 */
public class PumiceAskIfNeedElseStatementHandler implements PumiceUtteranceIntentHandler {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private SugiliteData sugiliteData;
    private String boolExpReadableName;
    final private SugiliteConditionBlock originalConditionBlock;

    public PumiceAskIfNeedElseStatementHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteConditionBlock originalConditionBlock, String boolExpReadableName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.originalConditionBlock = originalConditionBlock;
        this.boolExpReadableName = boolExpReadableName;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.CONFIRM_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage(context.getString(R.string.ask_if_else_statement_needed, boolExpReadableName.replace(" is true", "")), true, true);
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_POSITIVE)) {
            // else statement needed
            PumiceUserExplainElseStatementIntentHandler pumiceUserExplainElseStatementIntentHandler = new PumiceUserExplainElseStatementIntentHandler(pumiceDialogManager, context, sugiliteData, originalConditionBlock, boolExpReadableName);
            dialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainElseStatementIntentHandler);
            pumiceUserExplainElseStatementIntentHandler.sendPromptForTheIntentHandler();
        }

        else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            // no else statement needed
            synchronized (originalConditionBlock) {
                originalConditionBlock.notify();
            }
            //set the intent handler back to the default one
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
        }

        else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.not_recognized_ask_for_binary), true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.PARSE_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.PARSE_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

}
