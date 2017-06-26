package com.github.hrhdaniel.config;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

@Path("/")
@Scanned
public class ConfigResource {

	@ComponentImport
	private final UserManager userManager;

	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;

	@ComponentImport
	private final TransactionTemplate transactionTemplate;

	@Inject
	public ConfigResource(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,
			TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		return Response.ok(transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
				Config config = new Config();
				config.setVaultUrl((String) settings.get("CfnDeployment.vaultUrl"));
				config.setVaultToken((String) settings.get("CfnDeployment.vaultToken"));
				config.setVaultKeyPath((String) settings.get("CfnDeployment.vaultKeyPath"));
				return config;
			}
		})).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final Config config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
				pluginSettings.put("CfnDeployment.vaultUrl", config.getVaultUrl());
				pluginSettings.put("CfnDeployment.vaultToken", config.getVaultToken());
				pluginSettings.put("CfnDeployment.vaultKeyPath", config.getVaultKeyPath());
				return null;
			}
		});
		return Response.noContent().build();
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class Config {
		@XmlElement
		private String vaultUrl;
		@XmlElement
		private String vaultToken;
		@XmlElement
		private String vaultKeyPath;

		public String getVaultUrl() {
			return vaultUrl;
		}

		public void setVaultUrl(String vaultUrl) {
			this.vaultUrl = vaultUrl;
		}

		public String getVaultToken() {
			return vaultToken;
		}

		public void setVaultToken(String vaultToken) {
			this.vaultToken = vaultToken;
		}

		public String getVaultKeyPath() {
			return vaultKeyPath;
		}

		public void setVaultKeyPath(String vaultKeyPath) {
			this.vaultKeyPath = vaultKeyPath;
		}
	}
}
