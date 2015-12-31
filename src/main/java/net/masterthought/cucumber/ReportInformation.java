package net.masterthought.cucumber;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import net.masterthought.cucumber.json.Feature;
import net.masterthought.cucumber.json.Match;
import net.masterthought.cucumber.json.Result;
import net.masterthought.cucumber.json.Scenario;
import net.masterthought.cucumber.json.Step;
import net.masterthought.cucumber.json.Tag;
import net.masterthought.cucumber.json.support.ResultsWithMatch;
import net.masterthought.cucumber.json.support.Status;
import net.masterthought.cucumber.json.support.StatusCounter;
import net.masterthought.cucumber.json.support.StepObject;
import net.masterthought.cucumber.json.support.TagObject;
import net.masterthought.cucumber.util.Util;

public class ReportInformation {

    private final List<Feature> allFeatures;
    private long allDurations;
    private long allTagDuration;
    private int allTagSteps;

    private final Map<String, TagObject> allTags = new TreeMap<>();
    private final Map<String, StepObject> allSteps = new HashMap<>();

    private final StatusCounter tagsStatusCounter = new StatusCounter();
    private final StatusCounter tagsCounter = new StatusCounter();
    private final StatusCounter scenarioCounter = new StatusCounter();
    private final StatusCounter stepStatusCounter = new StatusCounter();

    public ReportInformation(List<Feature> features) {
        this.allFeatures = features;

        processFeatures();
    }

    public List<Feature> getAllFeatures() {
        return allFeatures;
    }

    public List<TagObject> getAllTags() {
        return new ArrayList<>(allTags.values());
    }

    public List<StepObject> getAllSteps() {
        return new ArrayList<>(allSteps.values());

    }

    public StatusCounter getAllScenarios() {
        return scenarioCounter;
    }

    public StatusCounter getStepsCounter() {
        return stepStatusCounter;
    }

    public int getAllPassedSteps() {
        return stepStatusCounter.getValueFor(Status.PASSED);
    }

    public int getAllFailedSteps() {
        return stepStatusCounter.getValueFor(Status.FAILED);
    }

    public int getAllSkippedSteps() {
        return stepStatusCounter.getValueFor(Status.SKIPPED);
    }

    public int getPendingStepsl() {
        return stepStatusCounter.getValueFor(Status.PENDING);
    }

    public int getTotalStepsMissing() {
        return stepStatusCounter.getValueFor(Status.MISSING);
    }

    public int getUndefinedSteps() {
        return stepStatusCounter.getValueFor(Status.UNDEFINED);
    }

    public String getAllDurationsAsString() {
        return Util.formatDuration(allDurations);
    }

    public Long getAllDurations() {
        return allDurations;
    }

    public String timeStamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
    }

    public int getAllTagScenarios() {
        return tagsCounter.size();
    }

    public int getAllPassedTagScenarios() {
        return tagsCounter.getValueFor(Status.PASSED);
    }

    public int getAllFailedTagScenarios() {
        return tagsCounter.getValueFor(Status.FAILED);
    }

    public int getAllTagSteps() {
        return allTagSteps;
    }

    public int getAllPassesTags() {
        return tagsStatusCounter.getValueFor(Status.PASSED);
    }

    public int getAllFailsTags() {
        return tagsStatusCounter.getValueFor(Status.FAILED);
    }

    public int getAllSkippedTags() {
        return tagsStatusCounter.getValueFor(Status.SKIPPED);
    }

    public int getAllPendingTags() {
        return tagsStatusCounter.getValueFor(Status.PENDING);
    }

    public int getAllUndefinedTags() {
        return tagsStatusCounter.getValueFor(Status.UNDEFINED);
    }

    public int getAllMissingTags() {
        return tagsStatusCounter.getValueFor(Status.MISSING);
    }

    public long getAllTagDuration() {
        return allTagDuration;
    }

    public int getAllPassedScenarios() {
        return scenarioCounter.getValueFor(Status.PASSED);
    }

    public int getAllFailedScenarios() {
        return scenarioCounter.getValueFor(Status.FAILED);
    }

    private void processFeatures() {
        for (Feature feature : allFeatures) {

            for (Scenario scenario : feature.getScenarios()) {
                scenarioCounter.incrementFor(scenario.getStatus());

                for (Tag tag : scenario.getTags()) {
                    tagsCounter.incrementFor(scenario.getStatus());

                    TagObject tagObject = addTagObject(tag.getName());
                    processTag(tagObject, scenario);
                }

                Step[] steps = scenario.getSteps();
                for (Step step : steps) {
                    stepStatusCounter.incrementFor(step.getStatus());
                    allDurations += step.getDuration();
                }
                countSteps(steps);

                countSteps(scenario.getBefore());
                countSteps(scenario.getAfter());
            }
        }
    }

    private void processTag(TagObject tag, Scenario scenario) {
        tag.addScenarios(scenario);

        Step[] steps = scenario.getSteps();
        for (Step step : steps) {
            tagsStatusCounter.incrementFor(step.getStatus());
            allTagDuration += step.getDuration();
        }
        allTagSteps += steps.length;
    }

    private void countSteps(ResultsWithMatch[] steps) {
        for (ResultsWithMatch step : steps) {

            Match match = step.getMatch();
            // no match = could not find method that was matched to this step -> status is missing
            if (match == null) {
                continue;
            }

            String methodName = match.getLocation();
            // location is missing so there is no way to identify step
            if (StringUtils.isEmpty(methodName)) {
                continue;
            }

            StepObject stepObject = allSteps.get(methodName);
            // if first occurrence of this location add element to the map
            if (stepObject == null) {
                stepObject = new StepObject(methodName);
            }
            // happens that report is not valid - does not contain information about result
            Result result = step.getResult();
            if (result != null) {
                stepObject.addDuration(result.getDuration(), result.getStatus());
            } else {
                // when result is not available it means that something really went wrong (report is incomplete)
                // and for this case FAILED status is used to avoid problems during parsing
                stepObject.addDuration(0, Status.FAILED.name());
            }
            allSteps.put(methodName, stepObject);
        }
    }

    private TagObject addTagObject(String name) {
        TagObject tagObject = allTags.get(name);
        if (tagObject == null) {
            tagObject = new TagObject(name);
            allTags.put(tagObject.getTagName(), tagObject);
        }
        return tagObject;
    }
}
