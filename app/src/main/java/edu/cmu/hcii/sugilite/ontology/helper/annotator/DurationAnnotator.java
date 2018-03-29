package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given input as a string containing duration (with keywords hr, min, etc.), parse the duration and
 * store it with millimeter unit.
 *
 * Created by shi on 3/1/18.
 */

public class DurationAnnotator extends SugiliteTextAnnotator {
    public DurationAnnotator() {super();}

    static final int DAY = 86400000;
    static final int HOUR = 3600000;
    static final int MINUTE = 60000;
    static final int SECOND = 1000;

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b\\d+?(.\\d+?)? (d|h|hr(s)?|hour(s)?|m|min(s)?|minute(s)?|s|sec)\\b";
        Pattern pattern = Pattern.compile(regex);

        int curEnd = -3;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split(" ");
            double num = Double.valueOf(parsed[0]);
            if (parsed[1].startsWith("h")) num *= HOUR;
            else if (parsed[1].startsWith("d")) num *= DAY;
            else if (parsed[1].startsWith("m")) num *= MINUTE;
            else if (parsed[1].startsWith("s")) num *= SECOND;
            if (matcher.start() - curEnd == 1 && text.charAt(curEnd) == ' ') {
                AnnotatingResult last = results.get(results.size() - 1);
                last.setNumericValue(last.getNumericValue() + num);
            }
            else {
                AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                        matcher.start(), matcher.end(), num);
                results.add(res);
            }
            curEnd = matcher.end();
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_DURATION;
}
