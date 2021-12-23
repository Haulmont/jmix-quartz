package io.jmix.quartz.util;

import io.jmix.core.impl.scanning.ClasspathScanCandidateDetector;
import io.jmix.core.impl.scanning.JmixModulesClasspathScanner;
import org.quartz.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component("quartz_QuartzUtils")
public class QuartzUtils {

    @Autowired
    private JmixModulesClasspathScanner classpathScanner;

    public List<String> getExistedJobsClassNames() {
        return new ArrayList<>(classpathScanner.getClassNames(QuartzJobDetector.class));
    }

    @Component("quartz_QuartzJobDetector")
    private static class QuartzJobDetector implements ClasspathScanCandidateDetector {
        @Override
        public boolean isCandidate(MetadataReader metadataReader) {
            return Arrays.asList(metadataReader.getClassMetadata().getInterfaceNames()).contains(Job.class.getName());
        }
    }

}
