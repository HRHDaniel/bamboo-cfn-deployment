This plugin for Atlassian Bamboo provides a deployment task to deploy CloudFormation templates to Amazon.  (This is a branch that stores the AWS credentials in Hashicorp vault instead of within Bamboo's global plugin settings).

# Setup
After installing the plugin, log in as a bamboo administrator and from the administration screens, find the `CFN Deployment Admin` link from the left hand menu.  From that screen enter details for connecting to Vault to pull the AWS credentials be used for deploying CFN templates.

# Usage
On any deployment project, add a `Deploy CFN Template` task. On that task provide the `stack name` (required) and the `path` to the template files (optional: default is the root/working directory of the deployment project).

The task will look for a file with the stackname and an extension of .json, .yml, .yaml

The stack name created in AWS will be suffixed with a dash and the environment name (-dev, -qa, -prod, etc.)

The plugin will apply additional files for the stack located within the same directory.  **Currently only json is support for the supporting files.**  All supporting files are optional for the plugin (your stack may require parameters and fail if they are not supplied, but that error will be from the call to create the cloud formation stack and not enforced as a requirement by the plugin.)

#### Parameter files:
- {stack_name}-parameters-common.json : parameters passed to the cloud formation call regardless of the environment.
- {stack_name}-parameters-{environment_name}.json : parameters passed to the cloud formation call based on the environment being deployed to.  These will overwrite "common" parameters.
- {stack_name}-versions.json : Contains parameters and is applied last overwriting both the common and the environment ones.  This is intended to pass versions of software installed by the template, thus promoting the same versions through each environment, while separating those into a file for easy readability.

Example:
```json
[
  {
    "parameterKey" : "ParameterNamedA",
    "parameterValue" : "5",
    "usePreviousValue" : true,
  },
  {
    "parameterKey" : "ParameterNamedB",
    "parameterValue" : "B_value",
    "usePreviousValue" : false,
  }
]
```

#### Tag files:
- {stack_name}-tags-common.json : tags passed to the cloud formation call regardless of the environment.
- {stack_name}-tags-{environment_name}.json : tags passed to the cloud formation call based on the environment being deployed to.  These will overwrite "common" parameters.

Example:
```json
[
  {
    "key" : "TagNamedA",
    "value" : "5"
  },
  {
    "key" : "TagNamedB",
    "value" : "B_value"
  }
]
```

#### File List
Given a stack name of "jolly-good-stack" being deployed to dev, all possible files used would be:
- jolly-good-stack.json
- jolly-good-stack-parameters-common.json
- jolly-good-stack-parameters-dev.json
- jolly-good-stack-versions.json
- jolly-good-stack-tags-common.json
- jolly-good-stack-tags-dev.json
 
# Plugin development:

Here are the SDK commands you'll use immediately:

* atlas-run   -- installs this plugin into the product and starts it on localhost
* atlas-debug -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-cli   -- after atlas-run or atlas-debug, opens a Maven command line window:
                 - 'pi' reinstalls the plugin into the running product instance
* atlas-help  -- prints description for all commands in the SDK

Full documentation is always available at:

[Getting Started - Atlassian Developers](https://developer.atlassian.com/docs/getting-started)


Additionally, the following were helpful in creation of the plugin:

[Atlassian plugin development - writing a task with a user interface](https://developer.atlassian.com/bamboodev/bamboo-tasks-api/writing-a-task-with-a-user-interface)  
[Atlassian plugin development - creating an admin configuration form](https://developer.atlassian.com/docs/common-coding-tasks/creating-an-admin-configuration-form)  
[Atlassian plugin development - introduction to 5.0 deployments](https://developer.atlassian.com/bamboodev/bamboo-plugin-guide/introduction-to-5-0-deployments)  
[Atlassian plugin development - api javadoc](https://docs.atlassian.com/atlassian-bamboo/latest/)  
[AWS Java SDK javadoc](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html)  
[AWS Java SDK developer guide](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html)  
[AWS Cloud formation API reference](https://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_Operations.html)  
[AWS Cloud formation CLI reference](https://docs.aws.amazon.com/cli/latest/reference/cloudformation/)  
