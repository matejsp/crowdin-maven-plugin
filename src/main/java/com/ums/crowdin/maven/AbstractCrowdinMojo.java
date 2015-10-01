package com.ums.crowdin.maven;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public abstract class AbstractCrowdinMojo extends AbstractMojo {

	private static final String HTTP_AUTH_NTLM_DOMAIN = "http.auth.ntlm.domain";

	private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";

	private static final String HTTP_PROXY_USER = "http.proxyUser";

	private static final String HTTP_PROXY_PORT = "http.proxyPort";

	private static final String HTTP_PROXY_HOST = "http.proxyHost";

	protected static final SAXBuilder saxBuilder = new SAXBuilder();

	/**
	 * The current Maven project
	 *
	 * @parameter default-value="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject project;

	protected void setProject(MavenProject value) {
		project = value;
	}

	/**
	 * The folder where the language files are located in the project.
	 *
	 * @parameter
	 * @required
	 */
	protected File languageFilesFolder;

	protected void setLanguageFilesFolder(File value) {
		languageFilesFolder = value;
	}

	/**
	 * The folder where the downloaded language files should be placed.
	 *
	 * @parameter
	 * @required
	 */
	protected File downloadFolder;

	protected void setDownloadFolder(File value) {
		downloadFolder = value;
	}

	/**
	 * The file where the translations status/statistics should be stored.
	 *
	 * @parameter
	 */
	protected File statusFile;

	protected void setStatusFile(File value) {
		statusFile = value;
	}

	/**
	 * The Maven Wagon manager to use when obtaining server authentication details.
	 *
	 * @component role="org.apache.maven.artifact.manager.WagonManager"
	 */
	protected WagonManager wagonManager;

	protected void setWagonManager(WagonManager value) {
		wagonManager = value;
	}

	/**
	 * Maven ProjectHelper.
	 *
	 * @component
	 */
	protected MavenProjectHelper projectHelper;

	/**
	 *
	 * Server id in settings.xml. &lt;username&gt; is project identifier, &lt;password&gt; is API key
	 *
	 * @parameter property = "crowdinServerId"
	 * @required
	 */
	protected String crowdinServerId;

	protected void setCrowdinServerId(String value) {
		crowdinServerId = value;
	}

	protected DefaultHttpClient client;
	protected AuthenticationInfo authenticationInfo;

	public void execute() throws MojoExecutionException, MojoFailureException {
		authenticationInfo = wagonManager.getAuthenticationInfo(crowdinServerId);
		if (authenticationInfo == null || authenticationInfo.getUserName() == null || authenticationInfo.getPassword() == null) {
			throw new MojoExecutionException("Failed to find server with id " + crowdinServerId
					+ " in Maven settings (~/.m2/settings.xml)");
		}

		client = new DefaultHttpClient();
		if (System.getProperty(HTTP_PROXY_HOST) != null) {
			String host = System.getProperty(HTTP_PROXY_HOST);
			String port = System.getProperty(HTTP_PROXY_PORT);

			if (port == null) {
				throw new MojoExecutionException("http.proxyHost without http.proxyPort");
			}
			HttpHost proxy = new HttpHost(host, Integer.parseInt(port));
			client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

			Credentials credential = null;

			String user = System.getProperty(HTTP_PROXY_USER);
			String password = System.getProperty(HTTP_PROXY_PASSWORD);

			if (System.getProperty(HTTP_AUTH_NTLM_DOMAIN) != null) {
				String domain = System.getProperty(HTTP_AUTH_NTLM_DOMAIN);
				if (user == null || password == null) {
					throw new MojoExecutionException(
							"http.auth.ntlm.domain without http.proxyUser and http.proxyPassword");
				}
				credential = new NTCredentials(user, password, host, domain);
			} else {
				if (user != null || password != null) {
					if (user == null || password == null) {
						throw new MojoExecutionException("http.proxyUser and http.proxyPassword go together");
					}
					credential = new UsernamePasswordCredentials(user, password);
				}
			}
			if (credential != null) {
				AuthScope authScope = new AuthScope(null, -1);
				client.getCredentialsProvider().setCredentials(authScope, credential);
			}
		}

	}

	protected boolean crowdinContainsFile(Element files, String fileName, boolean folder) {
		getLog().debug("Check that crowdin project contains " + fileName);
		@SuppressWarnings("unchecked")
		List<Element> items = files.getChildren("item");
		int slash = fileName.indexOf('/');
		if (slash == -1) {
			if (folder) {
				Element folderElement = crowdinGetFolder(items, fileName);
				if (folderElement != null) {
					getLog().debug("Crowdin project contains " + fileName);
					return true;
				}
			} else {
				for (Element item : items) {
					if (fileName.equals(item.getChildTextNormalize("name"))) {
						getLog().debug("Crowdin project contains " + fileName);
						return true;
					}
				}
			}
		} else {
			String folderName = fileName.substring(0, slash);
			String subPath = fileName.substring(slash + 1);
			Element folderElement = crowdinGetFolder(items, folderName);
			if (folderElement != null) {
				Element subFiles = folderElement.getChild("files");
				return crowdinContainsFile(subFiles, subPath, folder);
			}
		}
		getLog().debug("Crowdin project does not contain " + fileName);
		return false;
	}

	protected boolean crowdinContainsFile(Element files, String fileName) {
		return crowdinContainsFile(files, fileName, false);
	}

	protected boolean crowdinContainsFolder(Element files, String fileName) {
		return crowdinContainsFile(files, fileName, true);
	}

	protected Element crowdinGetFolder(List<Element> items, String fileName) {
		for (Element item : items) {
			if (fileName.equals(item.getChildTextNormalize("name"))) {
				if (crowdinIsFolder(item)) {
					return item;
				}
			}
		}
		return null;
	}

	protected boolean crowdinIsFolder(Element item) {
		return item.getChild("node_type") != null && "directory".equals(item.getChildTextNormalize("node_type"));
	}

	protected Document crowdinRequestAPI(String method, Map<String, String> parameters, Map<String, File> files,
			boolean mustSucceed) throws MojoExecutionException {
		return crowdinRequestAPI(method, parameters, files, null, null, mustSucceed);
	}

	protected Document crowdinRequestAPI(String method, Map<String, String> parameters, Map<String, File> files,
			Map<String, String> titles, Map<String, String> patterns, boolean mustSucceed) throws MojoExecutionException {
		try {
			String uri = "http://api.crowdin.net/api/project/" + authenticationInfo.getUserName() + "/" + method
					+ "?key=";
			getLog().debug("Calling " + uri + "<API Key>");
			uri += authenticationInfo.getPassword();
			HttpPost postMethod = new HttpPost(uri);

			MultipartEntity reqEntity = new MultipartEntity();

			if (parameters != null) {
				Set<Entry<String, String>> entrySetParameters = parameters.entrySet();
				for (Entry<String, String> entryParameter : entrySetParameters) {
					reqEntity.addPart(entryParameter.getKey(), new StringBody(entryParameter.getValue()));
				}
			}
			if (files != null) {
				Set<Entry<String, File>> entrySetFiles = files.entrySet();
				for (Entry<String, File> entryFile : entrySetFiles) {
					String key = "files[" + entryFile.getKey() + "]";
					reqEntity.addPart(key, new FileBody(entryFile.getValue()));
				}
			}

			if (titles != null) {
				Set<Entry<String, String>> entrySetTitles = titles.entrySet();
				for (Entry<String, String> entryTitle : entrySetTitles) {
					reqEntity.addPart("titles[" + entryTitle.getKey() + "]", new StringBody(entryTitle.getValue()));
				}
			}

			if (patterns != null) {
				Set<Entry<String, String>> entrySetPatterns = patterns.entrySet();
				for (Entry<String, String> entryPattern : entrySetPatterns) {
					reqEntity.addPart("export_patterns[" + entryPattern.getKey() + "]", new StringBody(entryPattern.getValue()));
				}
			}

			postMethod.setEntity(reqEntity);

			// getLog().debug("Sent request : ");
			// ByteArrayOutputStream bos = new ByteArrayOutputStream();
			// reqEntity.writeTo(bos);
			// getLog().debug(bos.toString());

			HttpResponse response = client.execute(postMethod);
			int returnCode = response.getStatusLine().getStatusCode();
			getLog().debug("Return code : " + returnCode);
			InputStream responseBodyAsStream = response.getEntity().getContent();
			Document document = saxBuilder.build(responseBodyAsStream);
			if (mustSucceed && document.getRootElement().getName().equals("error")) {
				String code = document.getRootElement().getChildTextNormalize("code");
				String message = document.getRootElement().getChildTextNormalize("message");
				throw new MojoExecutionException("Failed to call API - " + code + " - " + message);
			}
			return document;
		} catch (Exception e) {
			throw new MojoExecutionException("Failed to call API", e);
		}
	}

	protected String getMavenId(Artifact artifact) {
		return artifact.getGroupId() + "." + artifact.getArtifactId();
	}

}
