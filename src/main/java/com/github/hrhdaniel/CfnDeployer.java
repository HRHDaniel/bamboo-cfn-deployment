package com.github.hrhdaniel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CfnDeployer {

	public static CfnDeployer build(String accessKey, String secretKey, String defaultRegion) {
		return new CfnDeployer(accessKey, secretKey, defaultRegion);
	}

	private final BasicAWSCredentials awsCreds;
	private final String defaultRegion;

	private CfnDeployer(String accessKey, String secretKey, String defaultRegion) {
		awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		this.defaultRegion = defaultRegion;
	}

	public Stack getStack(String stackName) {
		AmazonCloudFormation client = AmazonCloudFormationClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(defaultRegion).build();

		DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
		DescribeStacksResult result = client.describeStacks(request);
		if (result.getStacks().size() > 1) {
			throw new RuntimeException("Found multiple stacks when searching for single stack with name: " + stackName);
		}
		return result.getStacks().get(0);
	}

	public void createOrUpdateStack(String baseStackName, String env, Path basePath) {
		try {
			AmazonCloudFormation client = AmazonCloudFormationClientBuilder.standard()
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(defaultRegion).build();

			String fullStackName = baseStackName + "-" + env;

			if ( !doesStackExist(fullStackName) ) {
				CreateStackRequest request = new CreateStackRequest();
				request.setStackName(fullStackName);
				request.setTemplateBody(getCfnContent(baseStackName, basePath));
				request.setTags(getTags(baseStackName, basePath));
				request.setParameters(getParameters(baseStackName, basePath));
				client.createStack(request);

				waitForCompletion(client.waiters().stackCreateComplete(), fullStackName);
			} else {
				UpdateStackRequest request = new UpdateStackRequest();
				request.setStackName(fullStackName);
				request.setTemplateBody(getCfnContent(baseStackName, basePath));
				request.setTags(getTags(baseStackName, basePath));
				request.setParameters(getParameters(baseStackName, basePath));
				client.updateStack(request);

				waitForCompletion(client.waiters().stackUpdateComplete(), fullStackName);
			}

		} catch (RuntimeException e) {
			// Don't wrap existing RTE's
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Collection<Parameter> getParameters(String stackName, Path basePath)
			throws JsonParseException, JsonMappingException, IOException {
		Map<String, Parameter> parameters = new HashMap<>();
		addParameters(parameters, basePath, stackName, "parameters-common");
		addParameters(parameters, basePath, stackName, "parameters-dev");
		addParameters(parameters, basePath, stackName, "versions");
		return parameters.values();
	}

	private void addParameters(Map<String, Parameter> parameters, Path basePath, String stackName, String postFix)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		File parameterFile = basePath.resolve(stackName + "-" + postFix + ".json").toFile();

		List<Parameter> fileParameters = (parameterFile.exists())
				? mapper.readValue(parameterFile, new TypeReference<List<Parameter>>() {
				}) : new ArrayList<>();

		fileParameters.forEach(p -> {
			parameters.put(p.getParameterKey(), p);
		});
	}

	private Collection<Tag> getTags(String stackName, Path basePath)
			throws JsonParseException, JsonMappingException, IOException {
		Map<String, Tag> tags = new HashMap<>();
		addTags(tags, basePath, stackName, "tags-common");
		addTags(tags, basePath, stackName, "tags-dev");
		return tags.values();
	}

	private void addTags(Map<String, Tag> tags, Path basePath, String stackName, String postFix)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		File tagFile = basePath.resolve(stackName + "-" + postFix + ".json").toFile();

		List<Tag> fileTags = (tagFile.exists()) ? mapper.readValue(tagFile, new TypeReference<List<Tag>>() {
		}) : new ArrayList<>();

		fileTags.forEach(t -> {
			tags.put(t.getKey(), t);
		});
	}

	private void waitForCompletion(Waiter<DescribeStacksRequest> waiter, String stackName) {
		DescribeStacksRequest r = new DescribeStacksRequest().withStackName(stackName);
		WaiterParameters<DescribeStacksRequest> waiterParameters = new WaiterParameters<DescribeStacksRequest>(r);
		waiter.run(waiterParameters);
	}

	private String getCfnContent(String stackName, Path basePath) throws IOException {
		List<String> extensions = Arrays.asList("json", "yml", "yaml");
		Path cfn = extensions.stream().map(ext -> basePath.resolve(stackName + "-cfn." + ext))
				.filter(path -> path.toFile().exists()).findFirst()
				.orElseThrow(() -> new RuntimeException("CFN Template not found"));
		return new String(Files.readAllBytes(cfn));
	}

	private boolean doesStackExist(String stackName) {
		try {
			getStack(stackName);
		} catch (AmazonCloudFormationException e) {
			if (e.getMessage().startsWith("Stack with id " + stackName + " does not exist")) {
				return false;
			}
			throw e;
		}
		return true;
	}

}
