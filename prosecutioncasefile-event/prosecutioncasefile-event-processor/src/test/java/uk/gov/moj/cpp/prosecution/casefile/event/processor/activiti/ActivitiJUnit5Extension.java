package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import java.io.InputStream;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.test.JobTestHelper;
import org.activiti.engine.test.Deployment;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

public class ActivitiJUnit5Extension implements BeforeEachCallback, AfterEachCallback {

    private ProcessEngine processEngine;
    private String deploymentId;

    @Override
    public void beforeEach(final ExtensionContext context) {
        final InputStream cfgStream = ActivitiJUnit5Extension.class.getResourceAsStream("/activiti.cfg.xml");
        processEngine = ProcessEngineConfiguration
                .createProcessEngineConfigurationFromInputStream(cfgStream, "processEngineConfiguration")
                .buildProcessEngine();

        final Method testMethod = context.getRequiredTestMethod();
        final Deployment deployment = testMethod.getAnnotation(Deployment.class);
        if (deployment != null) {
            final RepositoryService repositoryService = processEngine.getRepositoryService();
            final org.activiti.engine.repository.DeploymentBuilder builder = repositoryService.createDeployment();
            for (final String resource : deployment.resources()) {
                builder.addClasspathResource(resource);
            }
            deploymentId = builder.deploy().getId();
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (deploymentId != null) {
            processEngine.getRepositoryService().deleteDeployment(deploymentId, true);
            deploymentId = null;
        }
        if (processEngine != null) {
            processEngine.close();
            processEngine = null;
        }
    }

    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    public RuntimeService getRuntimeService() {
        return processEngine.getRuntimeService();
    }

    public StandaloneProcessEngineConfiguration getProcessEngineConfiguration() {
        return (StandaloneProcessEngineConfiguration) processEngine.getProcessEngineConfiguration();
    }

    public void waitForJobExecutorToProcessAllJobs(final long maxMillisToWait, final long intervalMillis) {
        JobTestHelper.waitForJobExecutorToProcessAllJobs(
                processEngine.getProcessEngineConfiguration(),
                processEngine.getManagementService(),
                maxMillisToWait, intervalMillis);
    }
}
