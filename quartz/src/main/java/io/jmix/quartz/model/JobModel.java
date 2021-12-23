package io.jmix.quartz.model;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.JmixEntity;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private Date lastFireDate;

    private Date nextFireDate;

    private Boolean isActive;

    private Boolean isDisallowConcurrentExecution;

    private Map<String, String> jobDataParameters = new HashMap<>();

    public Boolean getIsDisallowConcurrentExecution() {
        return isDisallowConcurrentExecution;
    }

    public void setIsDisallowConcurrentExecution(Boolean isDisallowConcurrentExecution) {
        this.isDisallowConcurrentExecution = isDisallowConcurrentExecution;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Date getNextFireDate() {
        return nextFireDate;
    }

    public void setNextFireDate(Date nextFireDate) {
        this.nextFireDate = nextFireDate;
    }

    public Date getLastFireDate() {
        return lastFireDate;
    }

    public void setLastFireDate(Date lastFireDate) {
        this.lastFireDate = lastFireDate;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, String> getJobDataParameters() {
        return jobDataParameters;
    }

    public void setJobDataParameters(Map<String, String> jobDataParameters) {
        this.jobDataParameters = jobDataParameters;
    }
}
