package io.jmix.quartz.screen.jobs;

import io.jmix.quartz.model.JobModel;
import io.jmix.quartz.service.QuartzDataProvider;
import io.jmix.quartz.service.QuartzService;
import io.jmix.ui.Notifications;
import io.jmix.ui.action.Action;
import io.jmix.ui.component.GroupTable;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.screen.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

@UiController("JobModel.browse")
@UiDescriptor("job-model-browse.xml")
@LookupComponent("jobModelsTable")
public class JobModelBrowse extends StandardLookup<JobModel> {

    @Autowired
    private Notifications notifications;
    @Autowired
    private QuartzService quartzService;
    @Autowired
    private QuartzDataProvider quartzDataProvider;
    @Autowired
    private CollectionContainer<JobModel> jobModelsDc;
    @Autowired
    private GroupTable<JobModel> jobModelsTable;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        loadJobsData();
    }

    private void loadJobsData() {
        jobModelsDc.setItems(quartzDataProvider.getAllJobs());
    }

    @Install(to = "jobModelsTable.executeNow", subject = "enabledRule")
    private boolean jobModelsTableExecuteNowEnabledRule() {
        return !CollectionUtils.isEmpty(jobModelsTable.getSelected())
                && !jobModelsTable.getSelected().iterator().next().getIsActive();
    }

    @Install(to = "jobModelsTable.activate", subject = "enabledRule")
    private boolean jobModelsTableActivateEnabledRule() {
        if (CollectionUtils.isEmpty(jobModelsTable.getSelected())) {
            return false;
        }

        JobModel selectedJobModel = jobModelsTable.getSelected().iterator().next();
        return !selectedJobModel.getIsActive()
                && CollectionUtils.isNotEmpty(quartzDataProvider.getTriggersOfJob(selectedJobModel.getJobName(), selectedJobModel.getJobGroup()));
    }

    @Install(to = "jobModelsTable.deactivate", subject = "enabledRule")
    private boolean jobModelsTableDeactivateEnabledRule() {
        return !CollectionUtils.isEmpty(jobModelsTable.getSelected())
                && jobModelsTable.getSelected().iterator().next().getIsActive();
    }

    @Subscribe("jobModelsTable.executeNow")
    public void onJobModelsTableExecuteNow(Action.ActionPerformedEvent event) {
        if (CollectionUtils.isEmpty(jobModelsTable.getSelected())) {
            return;
        }

        JobModel selectedJobModel = jobModelsTable.getSelected().iterator().next();
        quartzService.executeNow(selectedJobModel.getJobName(), selectedJobModel.getJobGroup());
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withDescription("todo executed now")
                .show();

        loadJobsData();
    }

    @Subscribe("jobModelsTable.activate")
    public void onJobModelsTableActivate(Action.ActionPerformedEvent event) {
        if (CollectionUtils.isEmpty(jobModelsTable.getSelected())) {
            return;
        }

        JobModel selectedJobModel = jobModelsTable.getSelected().iterator().next();
        quartzService.activateJob(selectedJobModel.getJobName(), selectedJobModel.getJobGroup());
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withDescription("todo activated")
                .show();

        loadJobsData();
    }

    @Subscribe("jobModelsTable.deactivate")
    public void onJobModelsTableDeactivate(Action.ActionPerformedEvent event) {
        if (CollectionUtils.isEmpty(jobModelsTable.getSelected())) {
            return;
        }

        JobModel selectedJobModel = jobModelsTable.getSelected().iterator().next();
        quartzService.deactivateJob(selectedJobModel.getJobName(), selectedJobModel.getJobGroup());
        notifications.create(Notifications.NotificationType.HUMANIZED)
                .withDescription("todo deactivated")
                .show();

        loadJobsData();
    }

    @Subscribe("jobModelsTable.refresh")
    public void onJobModelsTableRefresh(Action.ActionPerformedEvent event) {
        loadJobsData();
    }

    @Install(to = "jobModelsTable.remove", subject = "enabledRule")
    private boolean jobModelsTableRemoveEnabledRule() {
        return !CollectionUtils.isEmpty(jobModelsTable.getSelected())
                && !jobModelsTable.getSelected().iterator().next().getIsActive();
    }

}