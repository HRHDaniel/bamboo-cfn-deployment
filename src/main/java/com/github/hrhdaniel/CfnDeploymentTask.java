package com.github.hrhdaniel;

import java.nio.file.Path;

import javax.inject.Inject;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.waiters.WaiterUnrecoverableException;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

@Scanned
public class CfnDeploymentTask implements DeploymentTaskType {

	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;

	@Inject
	public CfnDeploymentTask(PluginSettingsFactory pluginSettingsFactory) {
		this.pluginSettingsFactory = pluginSettingsFactory;
	}
	
	@Override
	public TaskResult execute(final DeploymentTaskContext taskContext) throws TaskException {
		
		final String baseStackName = taskContext.getConfigurationMap().get("stackName");

		Path workingDirectory = taskContext.getWorkingDirectory().toPath();
		Path basePath = workingDirectory.resolve(taskContext.getConfigurationMap().get("basePath"));
		
		String env = taskContext.getDeploymentContext().getEnvironmentName().toLowerCase();
		
		return createOrUpdateStack(baseStackName, basePath, env, taskContext);
	}

	private TaskResult createOrUpdateStack(final String baseStackName, Path basePath, String env, DeploymentTaskContext taskContext) {
		final BuildLogger buildLogger = taskContext.getBuildLogger();
		CfnDeployer cfn = createCfnDeployer();
		try {
			cfn.createOrUpdateStack(baseStackName, env, basePath);
			return TaskResultBuilder.newBuilder(taskContext).success().build();
		} catch (WaiterUnrecoverableException originalException) { 
			buildLogger.addErrorLogEntry("CFN Deployment failed or timed out with:");
			buildLogger.addErrorLogEntry("  WaiterUnrecoverableException: " + originalException.getMessage());
			try {
				Stack stack = cfn.getStack(baseStackName + "-" + env);
				buildLogger.addErrorLogEntry("- StackName: " + stack.getStackName());
				buildLogger.addErrorLogEntry("- StackId: " + stack.getStackId());
				buildLogger.addErrorLogEntry("- Status: " + stack.getStackStatus());
				buildLogger.addErrorLogEntry("- Status Reason: " + stack.getStackStatusReason());
				return TaskResultBuilder.newBuilder(taskContext).failed().build();
			} catch (Exception nestedException) {
				buildLogger.addErrorLogEntry("  Unable to obtain additional error reasons due to nested " + nestedException.getClass().getName() + ":" + nestedException.getMessage());
				throw originalException;
			}
		}
	}
	
	private CfnDeployer createCfnDeployer() {
		PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		
		final String region = (String) settings.get("CfnDeployment.awsDefaultRegion");
		final String accessKey = (String) settings.get("CfnDeployment.awsAccessKey");
		final String secretKey = (String) settings.get("CfnDeployment.awsSecretKey");
		
		return CfnDeployer.build(accessKey, secretKey, region);
	}
}
