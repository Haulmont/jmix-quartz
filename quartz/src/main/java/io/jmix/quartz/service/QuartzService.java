package io.jmix.quartz.service;

import io.jmix.quartz.model.JobDataParameterModel;
import io.jmix.quartz.model.JobModel;
import io.jmix.quartz.model.ScheduleType;
import io.jmix.quartz.model.TriggerModel;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Serves as proxy from Jmix to the Quartz engine for manipulating with jobs
 */
@Service("quartz_QuartzService")
public class QuartzService {

    private static final Logger log = LoggerFactory.getLogger(QuartzService.class);

    @Autowired
    private Scheduler scheduler;

    /**
     * @param jobModel
     * @param jobDataParameterModels
     * @param triggerModels
     */
    @SuppressWarnings("unchecked")
    public void updateQuartzJob(JobModel jobModel,
                                List<JobDataParameterModel> jobDataParameterModels,
                                List<TriggerModel> triggerModels) {
        log.debug("updating job with name {} and group {}", jobModel.getJobName(), jobModel.getJobGroup());
        try {
            JobKey jobKey = JobKey.jobKey(jobModel.getJobName(), jobModel.getJobGroup());
            JobDetail existedJobDetail = scheduler.getJobDetail(jobKey);
            JobBuilder jobBuilder = getJobBuilder(jobModel, existedJobDetail);

            if (CollectionUtils.isNotEmpty(jobDataParameterModels)) {
                jobDataParameterModels.forEach(jobDataParameterModel ->
                        jobBuilder.usingJobData(jobDataParameterModel.getKey(), jobDataParameterModel.getValue()));
            }

            JobDetail jobDetail = jobBuilder.build();
            scheduler.addJob(jobDetail, true);

            List<? extends Trigger> existedTriggers = scheduler.getTriggersOfJob(jobKey);
            if (!CollectionUtils.isEmpty(triggerModels)) {
                for (TriggerModel triggerModel : triggerModels) {
                    TriggerKey newTriggerKey = TriggerKey.triggerKey(triggerModel.getTriggerName(), triggerModel.getTriggerGroup());
                    TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                            .withIdentity(newTriggerKey)
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
                    if (triggerModel.getEndDate() != null) {
                        triggerBuilder.endAt(triggerModel.getEndDate());
                    }

                    Trigger trigger = triggerBuilder.build();
                    scheduler.pauseTrigger(trigger.getKey());

                    Trigger existedTrigger = existedTriggers.stream()
                            .filter(t -> t.getKey().equals(newTriggerKey))
                            .findFirst()
                            .orElse(null);
                    if (existedTrigger == null) {
                        scheduler.scheduleJob(trigger);
                    } else {
                        scheduler.rescheduleJob(existedTrigger.getKey(), trigger);
                    }
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
    private JobBuilder getJobBuilder(JobModel jobModel, JobDetail existedJobDetail) throws ClassNotFoundException {
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
        return jobBuilder;
    }

    /**
     * Delegates to the Quartz engine resuming given job. This operation makes sense only for job with paused triggers.
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void activateJob(String jobName, String jobGroup) {
        log.debug("activating job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to activating job with name {} and group {}", jobName, jobGroup, e);
        }
    }

    /**
     * Delegates to the Quartz engine pausing given job. This operation makes sense only for jobs with active triggers.
     *
     * @param jobName  name of the job
     * @param jobGroup group of the job
     */
    public void deactivateJob(String jobName, String jobGroup) {
        log.debug("deactivating job with name {} and group {}", jobName, jobGroup);
        try {
            scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            log.warn("Unable to deactivating job with name {} and group {}", jobName, jobGroup, e);
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
