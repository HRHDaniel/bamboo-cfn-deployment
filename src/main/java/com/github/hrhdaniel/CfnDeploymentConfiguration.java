package com.github.hrhdaniel;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;

public class CfnDeploymentConfiguration extends AbstractTaskConfigurator {

	private static void require(String key, String value, String errorMessage, ErrorCollection errorCollection) {
		if (StringUtils.isEmpty(value)) {
			errorCollection.addError(key, errorMessage);
		}
	}

	@Override
	public Map<String, String> generateTaskConfigMap(ActionParametersMap params,
			TaskDefinition previousTaskDefinition) {
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

		config.put("stackName", params.getString("stackName"));
		config.put("basePath", params.getString("basePath"));

		return config;
	}

	@Override
	public void validate(final ActionParametersMap params, final ErrorCollection errorCollection) {
		super.validate(params, errorCollection);

		require("stackName", params.getString("stackName"), "Stack Name is required", errorCollection);
	}

	@Override
	public void populateContextForCreate(final Map<String, Object> context) {
		super.populateContextForCreate(context);
	}

	@Override
	public void populateContextForEdit(final Map<String, Object> context, final TaskDefinition taskDefinition) {
		super.populateContextForEdit(context, taskDefinition);
		context.put("stackName", taskDefinition.getConfiguration().get("stackName"));
		context.put("basePath", taskDefinition.getConfiguration().get("basePath"));
	}

}
