package com.github.hrhdaniel;

import java.nio.file.Path;

import javax.inject.Inject;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
		final String vaultUrl = (String) settings.get("CfnDeployment.vaultUrl");
		final String vaultToken = (String) settings.get("CfnDeployment.vaultToken");
		final String vaultKeyPath = (String) settings.get("CfnDeployment.vaultKeyPath");
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Vault-Token", vaultToken);
		String url = vaultUrl + "/v1/secret" + vaultKeyPath;
		
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(5000);
		RestTemplate rt = new RestTemplate(factory);
		
		HttpEntity<String> request = new HttpEntity<String>("", headers);
		
		ResponseEntity<VaultValues> response = rt.exchange(url, HttpMethod.GET, request, VaultValues.class);
		
		VaultValues body = response.getBody();

		String region = body.getData().get("AWS_DEFAULT_REGION");
		String accessKey = body.getData().get("AWS_ACCESS_KEY_ID");
		String secretKey = body.getData().get("AWS_SECRET_ACCESS_KEY");
		
		return CfnDeployer.build(accessKey, secretKey, region);
	}
}
