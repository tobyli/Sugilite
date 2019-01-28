package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */

public class PumiceDefaultUtteranceIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient Activity context;
    private transient PumiceDialogManager pumiceDialogManager;
    private Calendar calendar;

    public PumiceDefaultUtteranceIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance){
        String text = utterance.getContent().toLowerCase();

        //**test***
        if (text.contains("weather")) {
            return PumiceIntent.TEST_WEATHER;
        }

        if (text.contains("start again") || text.contains("start over")) {
            return PumiceIntent.START_OVER;
        }

        if (text.contains("undo") || text.contains("go back to the last") || text.contains("go back to the previous")) {
            return PumiceIntent.UNDO_STEP;
        }

        if (text.contains("show existing")) {
            return PumiceIntent.SHOW_KNOWLEDGE;
        }

        if (text.contains("show raw")) {
            return PumiceIntent.SHOW_RAW_KNOWLEDGE;
        }

        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    /**
     * handle the intent when the user is in the default state
     * @param dialogManager
     * @param pumiceIntent
     * @param utterance
     */
    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch (pumiceIntent) {
            case START_OVER:
                dialogManager.sendAgentMessage("I understand you want to start over: " + utterance.getContent(), true, false);
                dialogManager.startOverState();
                break;
            case UNDO_STEP:
                dialogManager.sendAgentMessage("I understand you want to undo: " + utterance.getContent(), true, false);
                dialogManager.revertToLastState();
                break;
            case TEST_WEATHER:
                ImageView imageView = new ImageView(context);
                imageView.setImageDrawable(context.getResources().getDrawable(R.mipmap.demo_card));
                dialogManager.sendAgentViewMessage(imageView, "Here is the weather", true, false);
                break;
            case USER_INIT_INSTRUCTION:
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent(), "ROOT");
                dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
                } catch (Exception e){
                    //TODO: error handling
                    e.printStackTrace();
                    pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
                }
                System.out.println(pumiceInstructionPacket.toString());
                break;
            case SHOW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the existing knowledge...", true, false);
                dialogManager.sendAgentMessage(dialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), false, false);
                break;
            case SHOW_RAW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the raw knowledge..." + dialogManager.getPumiceKnowledgeManager().getRawKnowledgeInString(), true, false);
                dialogManager.sendAgentMessage(dialogManager.getPumiceKnowledgeManager().getRawKnowledgeInString(), false, false);
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //handle server response from the semantic parsing server
        Gson gson = new Gson();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            if (resultPacket.utteranceType != null) {
                switch (PumiceUtteranceIntentHandler.PumiceIntent.valueOf(resultPacket.utteranceType)) {
                    case USER_INIT_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                pumiceDialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                pumiceDialogManager.sendAgentMessage(topResult.formula, false, false);

                                //do the parse on a new thread so it doesn't block the conversational I/O
                                pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        //parse and process the server response
                                        pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                });
                            } else {
                                throw new RuntimeException("empty formula");
                            }
                        } else {
                            throw new RuntimeException("empty server result");
                        }
                        break;
                    default:
                        throw new RuntimeException("wrong type of result");
                }
            }
        } catch (Exception e){
            //TODO: error handling
            pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);
            e.printStackTrace();
        }
    }

    @Override
    public void runOnMainThread(Runnable r) {
        pumiceDialogManager.runOnMainThread(r);
    }
}
