package io.jmix.quartz.service;

import io.jmix.core.UnconstrainedDataManager;
import io.jmix.quartz.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Serves as proxy from Jmix to the Quartz engine for fetch information about jobs and triggers and update them
 */
@Service("quartz_QuartzService")
public class QuartzService {

    private static final Logger log = LoggerFactory.getLogger(QuartzService.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private UnconstrainedDataManager dataManager;

    /**
     * Returns information about all configured quartz jobs with related triggers
     */
    public List<JobModel> getAllJobs() {
        List<JobModel> result = new ArrayList<>();

        try {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
                JobModel jobModel = dataManager.create(JobModel.class);
                jobModel.setJobName(jobKey.getName());
                jobModel.setJobGroup(jobKey.getGroup());
                jobModel.setJobDataParameters(getDataParamsOfJob(jobKey));

                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                jobModel.setJobClass(jobDetail.getJobClass().getName());

                List<TriggerModel> triggerModels = new ArrayList<>();
                List<? extends Trigger> jobTriggers = scheduler.getTriggersOfJob(jobKey);
                if (!CollectionUtils.isEmpty(jobTriggers)) {
                    boolean isActive = false;
                    for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
                        TriggerModel triggerModel = dataManager.create(TriggerModel.class);
                        triggerModel.setTriggerName(trigger.getKey().getName());
                        triggerModel.setTriggerGroup(trigger.getKey().getGroup());
                        triggerModel.setScheduleType(trigger instanceof SimpleTrigger ? ScheduleType.SIMPLE : ScheduleType.CRON_EXPRESSION);
                        triggerModel.setStartDate(trigger.getStartTime());
                        triggerModel.setLastFireDate(trigger.getPreviousFireTime());
                        triggerModel.setNextFireDate(trigger.getNextFireTime());

                        if (trigger instanceof CronTrigger) {
                            triggerModel.setCronExpression(((CronTrigger) trigger).getCronExpression());
                        } else if (trigger instanceof SimpleTrigger) {
                            SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
                            //because org.quartz.SimpleScheduleBuilder.withRepeatCount
                            triggerModel.setRepeatCount(simpleTrigger.getRepeatCount() + 1);
                            triggerModel.setRepeatInterval(simpleTrigger.getRepeatInterval());
                        }

                        if (scheduler.getTriggerState(trigger.getKey()) == Trigger.TriggerState.NORMAL) {
                            isActive = true;
                        }

                        triggerModels.add(triggerModel);
                    }

                    jobModel.setTriggers(triggerModels);
                    jobModel.setJobState(isActive ? JobState.NORMAL : JobState.PAUSED);
                }

                result.add(jobModel);
            }
        } catch (SchedulerException e) {
            log.warn("Unable to fetch information about active jobs", e);
        }

        return result;
    }

    /**
     * Returns given job's parameters
     *
     * @param jobKey key of job
     * @return parameters of given job
     */
    private List<JobDataParameterModel> getDataParamsOfJob(JobKey jobKey) {
        List<JobDataParameterModel> result = new ArrayList<>();

        try {
            scheduler.getJobDetail(jobKey)
                    .getJobDataMap()
                    .getWrappedMap()
                    .forEach((k, v) -> {
                        JobDataParameterModel dataParameterModel = dataManager.create(JobDataParameterModel.class);
                        dataParameterModel.setKey(k);
                        dataParameterModel.setValue(v == null ? "" : v.toString());
                        result.add(dataParameterModel);
                    });
        } catch (SchedulerException e) {
            log.warn("Unable to fetch information about parameters for job {}", jobKey, e);
        }

        return result;
    }

    /**
     * Returns names of all known JobDetail groups
     */
    public List<String> getJobGroupNames() {
        List<String> result = new ArrayList<>();

        try {
            result = scheduler.getJobGroupNames();
        } catch (SchedulerException e) {
            log.warn("Unable to fetch information about job group names", e);
        }

        return result;
    }

    /**
     * Returns names of all known Trigger groups
     */
    public List<String> getTriggerGroupNames() {
        List<String> result = new ArrayList<>();

        try {
            result = scheduler.getTriggerGroupNames();
        } catch (SchedulerException e) {
            log.warn("Unable to fetch information about trigger group names", e);
        }

        return result;
    }

    /**
     * Updates job in Quartz engine
     *
     * @param jobModel               job to edit
     * @param jobDataParameterModels parameters for job
     * @param triggerModels          triggers for job
     */
    @SuppressWarnings("unchecked")
    public void updateQuartzJob(JobModel jobModel,
                                List<JobDataParameterModel> jobDataParameterModels,
                                List<TriggerModel> triggerModels) {
        log.debug("updating job with name {} and group {}", jobModel.getJobName(), jobModel.getJobGroup());
        try {
            JobKey jobKey = JobKey.jobKey(jobModel.getJobName(), jobModel.getJobGroup());
            JobDetail jobDetail = buildJobDetail(jobModel, scheduler.getJobDetail(jobKey), jobDataParameterModels);
            scheduler.addJob(jobDetail, true);

            if (!CollectionUtils.isEmpty(triggerModels)) {
                //remove obsolete triggers
                for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
                    scheduler.unscheduleJob(trigger.getKey());
                }

                //recreate triggers
                for (TriggerModel triggerModel : triggerModels) {
                    Trigger trigger = buildTrigger(jobDetail, triggerModel);
                    scheduler.scheduleJob(trigger);
                }
            }
        } catch (SchedulerException e) {
            log.warn("Unable to update job with name {} and group {}", jobModel.getJobName(), jobModel.getJobGroup(), e);
        } catch (ClassNotFoundException e) {
            log.warn("Unable to find job class {}", jobModel.getJobClass());
            throw new IllegalArgumentException("Job class " + jobModel.getJobClass() + " not found");
        }

    }

    @SuppressWarnings("unchecked")
    private JobDetail buildJobDetail(JobModel jobModel, JobDetail existedJobDetail, List<JobDataParameterModel> jobDataParameterModels)
            throws ClassNotFoundException {
        JobBuilder jobBuilder;
        if (existedJobDetail != null) {
            jobBuilder = existedJobDetail.getJobBuilder();
        } else {
            Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(jobModel.getJobClass());
            jobBuilder = JobBuilder.newJob()
                    .withIdentity(jobModel.getJobName(), jobModel.getJobGroup())
                    .ofType(jobClass)
                    .storeDurably();
        }

        if (CollectionUtils.isNotEmpty(jobDataParameterModels)) {
            jobDataParameterModels.forEach(jobDataParameterModel ->
                    jobBuilder.usingJobData(jobDataParameterModel.getKey(), jobDataParameterModel.getValue()));
        }

        return jobBuilder.build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, TriggerModel triggerModel) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(triggerModel.getTriggerName(), triggerModel.getTriggerGroup()))
                .forJob(jobDetail);

        if (triggerModel.getScheduleType() == ScheduleType.CRON_EXPRESSION) {
            triggerBuilder.withSchedule(cronSchedule(triggerModel.getCronExpression()));
        } else {
            SimpleScheduleBuilder simpleScheduleBuilder = simpleSchedule()
                    .withIntervalInMilliseconds(triggerModel.getRepeatInterval());
            if (triggerModel.getRepeatCount() > 0) {
                //required trick because actual number of firing will be + 1, see org.quartz.SimpleScheduleBuilder.withRepeatCount
                simpleScheduleBuilder.withRepeatCount(triggerModel.getRepeatCount() - 1);
            }
            triggerBuilder.withSchedule(simpleScheduleBuilder);
        }

        if (triggerModel.getStartDate() != null) {
            triggerBuilder.startAt(triggerModel.getStartDate());
        } else {
            triggerBuilder.startNow();
        }

        if (triggerModel.getEndDate() != null) {
            triggerBuilder.endAt(triggerModel.getEndDate());
        }

        return triggerBuilder.build();
    }

    /**
     * Delegates to the Quartz engine resuming given job. This operation makes sense only for job with paused triggers.
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void resumeJob(String jobName, String jobGroup) {
        log.debug("resuming job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to resume job with name {} and group {}", jobName, jobGroup, e);
        }
    }

    /**
     * Delegates to the Quartz engine pausing given job. This operation makes sense only for jobs with active triggers.
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void pauseJob(String jobName, String jobGroup) {
        log.debug("pausing job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to pause job with name {} and group {}", jobName, jobGroup, e);
        }
    }

    /**
     * Delegates to the Quartz engine triggering given job (executing now)
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void executeNow(String jobName, String jobGroup) {
        log.debug("triggering job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.triggerJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to trigger job with name {} and group {}", jobName, jobGroup, e);
        }
    }

    /**
     * Delegates to the Quartz engine deleting given job
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void deleteJob(String jobName, String jobGroup) {
        log.debug("deleting job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to delete job with name {} and group {}", jobName, jobGroup, e);
        }
    }
}
