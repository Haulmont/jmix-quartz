package io.jmix.quartz.model;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JmixEntity
public class JobModel {

    @JmixGeneratedValue
    @JmixId
    private UUID id;

    @NotNull
    private String jobName;

    private String jobGroup;

    @NotNull
    private String jobClass;

    private JobState jobState;

    private List<TriggerModel> triggers;

    private List<JobDataParameterModel> jobDataParameters = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public JobState getJobState() {
        return jobState;
    }

    public void setJobState(JobState jobState) {
        this.jobState = jobState;
    }

    public List<TriggerModel> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<TriggerModel> triggers) {
        this.triggers = triggers;
    }

    public List<JobDataParameterModel> getJobDataParameters() {
        return jobDataParameters;
    }

    public void setJobDataParameters(List<JobDataParameterModel> jobDataParameters) {
        this.jobDataParameters = jobDataParameters;
    }

    @Transient
    @JmixProperty
    public String getTriggerDescription() {
        if (CollectionUtils.isEmpty(triggers)) {
            return null;
        }

        return triggers.stream()
                .map(TriggerModel::getScheduleDescription)
                .collect(Collectors.joining(", "));
    }

    @Transient
    @JmixProperty
    public Date getLastFireDate() {
        if (CollectionUtils.isEmpty(triggers)) {
            return null;
        }

        Date lastFireDate = null;
        for (TriggerModel triggerModel : triggers) {
            Date triggerLastFireDate = triggerModel.getLastFireDate();
            if (lastFireDate == null || (triggerLastFireDate != null && lastFireDate.before(triggerLastFireDate))) {
                lastFireDate = triggerLastFireDate;
            }
        }

        return lastFireDate;
    }

    @Transient
    @JmixProperty
    public Date getNextFireDate() {
        if (CollectionUtils.isEmpty(triggers)) {
            return null;
        }

        Date nextFireDate = null;
        for (TriggerModel triggerModel : triggers) {
            Date triggerNextFireDate = triggerModel.getNextFireDate();
            if (nextFireDate == null || (triggerNextFireDate != null && nextFireDate.after(triggerNextFireDate))) {
                nextFireDate = triggerNextFireDate;
            }
        }

        return nextFireDate;
    }

}
