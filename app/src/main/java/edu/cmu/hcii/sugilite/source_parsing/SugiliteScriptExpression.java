package edu.cmu.hcii.sugilite.source_parsing;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpression;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteDelaySpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSubscriptSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSpecialOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteLongClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteReadoutConstOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteSelectOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;

/**
 * @author toby
 * @date 3/18/18
 * @time 10:49 PM
 */

//represents a full standalone statement in the script
public class SugiliteScriptExpression<T> {
    private String operationName;
    private List<SugiliteScriptExpression> arguments = new ArrayList<>();

    //whether the expression is a constant
    private boolean isConstant;

    //value of the expression IF the expression is a constant
    private T constantValue;

    //the script content of the expression
    private String scriptContent;


    /**
     * construct a SugiliteScriptExpression from a SugiliteScriptNode
     * @param node
     * @return
     */
    static public SugiliteScriptExpression parse(SugiliteScriptNode node){
        if(node.getChildren().size() > 1 && node.getChildren().get(0).getValue().equals("call")){
            //operation
            SugiliteScriptExpression<String> result = new SugiliteScriptExpression<>();
            result.setConstant(false);
            result.setOperationName(node.getChildren().get(1).getValue());
            //set the String content of the script
            result.setScriptContent(node.getScriptContent());
            for(int i = 2; i < node.getChildren().size(); i ++){
                //parse the args
                result.addArgument(parse(node.getChildren().get(i)));
            }
            return result;
        }
        else {
            //constant
            SugiliteScriptExpression<String> result = new SugiliteScriptExpression<>();
            result.setConstant(true);
            if(node.getValue() != null) {
                result.setConstantValue(node.getValue());
            } else {
                //TODO: temp hack
                result.setConstantValue(node.getScriptContent());
            }
            result.setScriptContent(node.getScriptContent());
            return result;
        }
    }

    void extractVariableFromQuery(SerializableOntologyQuery query, SugiliteStartingBlock startingBlock){
        if(query.getObject() != null){
            for(SugiliteSerializableEntity sugiliteSerializableEntity : query.getObject()){
                if(sugiliteSerializableEntity.getEntityValue() instanceof String){
                    if(((String) sugiliteSerializableEntity.getEntityValue()).startsWith("@")){
                        String variableName = sugiliteSerializableEntity.getEntityValue().toString().substring(1);
                        if(! startingBlock.variableNameDefaultValueMap.containsKey(variableName)){
                            //need to add to the variable map
                            startingBlock.variableNameDefaultValueMap.put(variableName, new StringVariable(Variable.USER_INPUT, variableName, ""));
                        }
                    }
                }
            }
        }
        if(query.getSubQueries() != null){
            for(SerializableOntologyQuery serializableOntologyQuery : query.getSubQueries()){
                if(query != null){
                    extractVariableFromQuery(serializableOntologyQuery, startingBlock);
                }
            }
        }
    }

    /**
     * convert an SugiliteScriptExpression to a SugiliteBlock
     * @return
     */
    public SugiliteBlock toSugiliteBlock(SugiliteStartingBlock startingBlock, OntologyDescriptionGenerator descriptionGenerator){
        if(operationName == null){
            //not a valid operation
            return null;
        }

        if(SugiliteUnaryOperation.isUnaryOperation(operationName) && arguments.size() == 1){
            //is a unary operation
            SugiliteUnaryOperation operation = null;
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            switch (operationName) {
                case "click":
                    operation = new SugiliteClickOperation();
                    ((SugiliteClickOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(0).getScriptContent())));
                    break;
                case "long_click":
                    operation = new SugiliteLongClickOperation();
                    ((SugiliteLongClickOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(0).getScriptContent())));
                    break;
                case "select":
                    operation = new SugiliteSelectOperation();
                    ((SugiliteSelectOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(0).getScriptContent())));
                    break;
                case "readout_const":
                    operation = new SugiliteReadoutConstOperation();
                    ((SugiliteReadoutConstOperation)operation).setTextToReadout(arguments.get(0).getConstantValue().toString());
                    break;
                case "resolve_boolExp":
                    operation = new SugiliteResolveBoolExpOperation();
                    ((SugiliteResolveBoolExpOperation)operation).setText(arguments.get(0).getConstantValue().toString());
                    break;
                case "resolve_procedure":
                    operation = new SugiliteResolveProcedureOperation();
                    ((SugiliteResolveProcedureOperation)operation).setText(arguments.get(0).getConstantValue().toString());
                    break;
                case "resolve_valueQuery":
                    operation = new SugiliteResolveValueQueryOperation();
                    ((SugiliteResolveValueQueryOperation)operation).setText(arguments.get(0).getConstantValue().toString());
                    break;
            }
            operationBlock.setOperation(operation);
            SerializableOntologyQuery query = new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(0).getScriptContent()));

            //extract variables from the query
            extractVariableFromQuery(query, startingBlock);


            //set the description
            if(descriptionGenerator != null) {
                operationBlock.setDescription(descriptionGenerator.getDescriptionForOperation(operation, operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));
            } else {
                operationBlock.setDescription(operationBlock.toString());
            }
            if(operationBlock.getDescription() == null){
                operationBlock.setDescription(operationBlock.toString());
            }

            return operationBlock;
        }

        else if(SugiliteBinaryOperation.isBinaryOperation(operationName) && arguments.size() == 2){
            //is a binary operation
            SugiliteBinaryOperation operation = null;
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            //full binary operation with a query included
            switch (operationName) {
                case "read_out":
                    operation = new SugiliteReadoutOperation();
                    operation.setParameter0(arguments.get(0).getConstantValue().toString());
                    ((SugiliteReadoutOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(1).getScriptContent())));
                    break;
                case "set_text":
                    operation = new SugiliteSetTextOperation();
                    String text = stripQuote(arguments.get(0).getConstantValue().toString());
                    operation.setParameter0(text);
                    ((SugiliteSetTextOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(1).getScriptContent())));
                    if(text.startsWith("@") && text.length() > 1){
                        //is a variable
                        String variableName = text.substring(1);
                        if(! startingBlock.variableNameDefaultValueMap.containsKey(variableName)){
                            //need to add to the variable map
                            startingBlock.variableNameDefaultValueMap.put(variableName, new StringVariable(Variable.USER_INPUT, variableName, ""));
                        }
                    }
                    break;
                case "get":
                    operation = new SugiliteGetOperation();
                    operation.setParameter0(arguments.get(0).getConstantValue().toString());
                    operation.setParameter1(arguments.get(1).getConstantValue().toString());
                    break;
            }
            operationBlock.setOperation(operation);

            //set the description
            if (descriptionGenerator != null && operationBlock.getOperation().getDataDescriptionQueryIfAvailable() != null) {
                operationBlock.setDescription(descriptionGenerator.getDescriptionForOperation(operation, operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));
            } else {
                operationBlock.setDescription(operationBlock.toString());
            }
            if (operationBlock.getDescription() == null) {
                operationBlock.setDescription(operationBlock.toString());
            }

            return operationBlock;
        }

        else if(SugiliteTrinaryOperation.isTrinaryOperation(operationName) && arguments.size() == 3){
            //is a trinary operation
            SugiliteTrinaryOperation operation = null;
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            switch (operationName){
                case "load_as_variable":
                    operation = new SugiliteLoadVariableOperation();
                    String variableName = arguments.get(0).getConstantValue().toString();
                    operation.setParameter0(variableName);
                    operation.setParameter1(arguments.get(1).getConstantValue().toString());
                    //the query is the parameter 2
                    ((SugiliteLoadVariableOperation)operation).setQuery(new SerializableOntologyQuery(OntologyQuery.deserialize(arguments.get(2).getScriptContent())));
                    //add the variable to the variable map in the starting block
                    startingBlock.variableNameDefaultValueMap.put(variableName, new StringVariable(Variable.LOAD_RUNTIME, variableName, ""));
                    break;
            }
            operationBlock.setOperation(operation);

            //set the description
            if(descriptionGenerator != null) {
                operationBlock.setDescription(descriptionGenerator.getDescriptionForOperation(operation, operationBlock.getOperation().getDataDescriptionQueryIfAvailable()));
            } else {
                operationBlock.setDescription(operationBlock.toString());
            }
            if(operationBlock.getDescription() == null){
                operationBlock.setDescription(operationBlock.toString());
            }

            return operationBlock;
        }

        else if(operationName.contentEquals("SUGILITE_START") && arguments.size() == 1){
            SugiliteStartingBlock newStartingBlock = new SugiliteStartingBlock();
            newStartingBlock.setScriptName(stripQuote(arguments.get(0).getConstantValue().toString()));
            return newStartingBlock;
        }

        else if(operationName.contentEquals("special_go_home") && arguments.size() == 0){
            SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(new SugiliteSpecialOperation(SugiliteOperation.SPECIAL_GO_HOME));
            operationBlock.setDescription(operationBlock.toString());
            return operationBlock;
        }

        else if(operationName.contentEquals("run_script") && arguments.size() == 1){
            SugiliteSubscriptSpecialOperationBlock operationBlock = new SugiliteSubscriptSpecialOperationBlock();
            operationBlock.setSubscriptName(stripQuote(arguments.get(0).getConstantValue().toString()));
            operationBlock.setDescription(operationBlock.toString());
            return operationBlock;
        }

        else if(operationName.contentEquals("delay") && arguments.size() == 1){
            int delayTime = Double.valueOf(arguments.get(0).getConstantValue().toString()).intValue();
            SugiliteDelaySpecialOperationBlock operationBlock = new SugiliteDelaySpecialOperationBlock(delayTime);
            operationBlock.setDescription(operationBlock.toString());
            return operationBlock;
        }

        else if(operationName.contentEquals("if") && arguments.size() == 2) {
            SugiliteBlock ifBlock = arguments.get(1).toSugiliteBlock(startingBlock, descriptionGenerator);
            SugiliteBlock previousBlock = ifBlock.getPreviousBlock();
            SugiliteBooleanExpression booleanExpression = new SugiliteBooleanExpression(arguments.get(0));
            SugiliteBooleanExpressionNew booleanExpression2 = new SugiliteBooleanExpressionNew(arguments.get(0));

            SugiliteConditionBlock conditionBlock = new SugiliteConditionBlock(ifBlock,null, booleanExpression, previousBlock);
            //test purpose
            conditionBlock.setSugiliteBooleanExpressionNew(booleanExpression2);
            return conditionBlock;
        }

        else if(operationName.contentEquals("if") && arguments.size() == 3) {
            SugiliteBlock ifBlock = arguments.get(1).toSugiliteBlock(startingBlock, descriptionGenerator);
            SugiliteBlock elseBlock = arguments.get(2).toSugiliteBlock(startingBlock, descriptionGenerator);
            SugiliteBlock previousBlock = ifBlock.getPreviousBlock();

            SugiliteBooleanExpression booleanExpression = new SugiliteBooleanExpression(arguments.get(0));
            SugiliteBooleanExpressionNew booleanExpression2 = new SugiliteBooleanExpressionNew(arguments.get(0));

            SugiliteConditionBlock conditionBlock = new SugiliteConditionBlock(ifBlock, elseBlock, booleanExpression, previousBlock);
            //test purpose
            conditionBlock.setSugiliteBooleanExpressionNew(booleanExpression2);
            return conditionBlock;
        }

        return null;
    }

    private static String stripQuote(String s){
        if(s.startsWith("\"") && s.endsWith("\"") && s.length() >=2){
            return s.substring(1, s.length() - 1);
        } else {
            return s;
        }
    }

    public String getOperationName() {
        return operationName;
    }

    /**
     * create a constant SugiliteScriptExpression from a token in raw string
     * @param token
     * @return
     */
    static public SugiliteScriptExpression parseConstant(String token){
        //constant
        if(StringUtils.isNumeric(token)){
            SugiliteScriptExpression<Double> result = new SugiliteScriptExpression<>();
            result.setConstant(true);
            result.setConstantValue(Double.valueOf(token));
            return result;
        }

        else{
            SugiliteScriptExpression<String> result = new SugiliteScriptExpression<>();
            result.setConstant(true);
            result.setConstantValue(token);
            return result;
        }
    }

    static public String addQuoteToTokenIfNeeded(String s){
        if(s.contains(" ")){
            return "\"" + s + "\"";
        } else {
            return s;
        }
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public List<SugiliteScriptExpression> getArguments() {
        return arguments;
    }

    public void setArguments(List<SugiliteScriptExpression> arguments) {
        this.arguments = arguments;
    }

    public void addArgument(SugiliteScriptExpression argument){
        this.arguments.add(argument);
    }

    public T getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(T constantValue) {
        this.constantValue = constantValue;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public void setConstant(boolean constant) {
        isConstant = constant;
    }

    private static boolean isOperation(String token){
        return true;
    }


    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public String getScriptContent() {
        return scriptContent;
    }
}
