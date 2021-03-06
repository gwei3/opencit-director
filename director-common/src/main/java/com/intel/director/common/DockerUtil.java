package com.intel.director.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.director.common.exception.ConnectionFailException;
import com.intel.director.common.exception.DirectorException;
import com.intel.director.constants.Constants;
import com.intel.mtwilson.util.exec.Result;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.intel.mtwilson.util.exec.ExecUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DockerUtil {

    private static final Logger log = LoggerFactory.getLogger(DockerUtil.class);

    public static int dockerSave(String repository, String tag, String tarDestination, String name) {
        File dirForDockerTar = new File(tarDestination);
        if (!dirForDockerTar.exists()) {
            dirForDockerTar.mkdir();
        }
        return executeDockerCommands("save", "-o", tarDestination + File.separator + name, repository + ":" + tag);
    }

    public static int dockerPull(String repository, String tag) {
        return executeDockerCommands("pull", repository + ":" + tag);

    }

    public static int dockerRMI(String repository, String tag) {
        return executeDockerCommands("rmi", "-f", repository + ":" + tag);

    }

    public static int dockerTag(String repository, String tag, String newRepo, String newTag) {
        return executeDockerCommands("tag", repository + ":" + tag, newRepo + ":" + newTag);

    }

    public static int dockerLoad(String source_image) {
        return executeDockerCommands("load", "--input=" + source_image);

    }

    public static int executeDockerCommands(String... args) {
        int exitcode;
        if (args.length == 0) {
            log.error("docker command needs at least one argument");
            return 1;
        }

        log.info("\n Inside executeDockerCommands going to run docker " + args[0]);

        try {
            exitcode = DirectorUtil.executeCommandInExecUtil(Constants.DOCKER_EXECUTABLES, args);
        } catch (IOException e) {
            log.error("Error in executeDockerCommands docker " + args[0], e);
            exitcode = 1;
        }
        return exitcode;

    }

    public static boolean checkDockerHubConnection() throws ClientProtocolException, IOException {
        int result = -1;
        // / return 0 when able to connect hub and search even though serch
        // / result is empty
        result = executeDockerCommands("search", "busybox");
        if (result == 0) {
            return true;
        }
        return false;
    }

    public static boolean checkWithV2Api(String repo, String tag)
            throws DirectorException, ConnectionFailException {

        boolean repoTagExists = false;


        //Find by API: https://registry.hub.docker.com/v2/repositories/library/redis/tags/
        String url = "https://registry.hub.docker.com/v2/repositories/library/" + repo + "/tags/";


        String command = "curl -i -X GET " + url;
        log.info("doesRepoTagExistInDockerHub, running command::" + command);
        Result result = null;
        try {
            result = DirectorUtil.executeCommand("curl", "-i", "-X", "GET", url);
        } catch (IOException e) {
            log.error("Unable to execute curl command : {}", command);
            return false;
        }
        String resultJson = result.getStdout();

        log.info("result:" + result + " console output::" + resultJson);
        if (StringUtils.isNotBlank(resultJson) && resultJson.indexOf("{") != -1) {
            log.info("Result contains json");
            resultJson = resultJson.substring(resultJson.indexOf("{"));
            log.info("Extracted json : {}", resultJson);
        } else {
            return false;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        RepoSearchResult readValue = null;
        try {
            readValue = objectMapper.readValue(resultJson, RepoSearchResult.class);
        } catch (IOException e) {
            log.error("Unable to convert JSON : {} into the expected java object", resultJson);
            return false;
        }
        if(readValue!=null && readValue.getResults()!=null ){
        log.info("tags returned: {}", readValue.getResults().size());
        List<TagDetail> results = readValue.getResults();
        for (TagDetail tagDetail : results) {
            if (tag.equals(tagDetail.getName())) {
                repoTagExists = true;
                log.info("Found the image with repo : {} and tag : {}", repo, tag);
                break;
            }
        }
        }

        return repoTagExists;

    }
    
    
    public static boolean doesRepoTagExistInDockerHub(String repo, String tag)
    	    throws DirectorException, ConnectionFailException {
    	return (checkWithV2Api(repo,tag)||checkWithDockerSearch(repo,tag)||checkWithV1Api(repo,tag));
    
    }
    
    
    public static boolean checkWithDockerSearch(String repo, String tag)
    	    throws DirectorException, ConnectionFailException {

    	boolean repoTagExists = false;
    	log.info("Checking using docker search command");

    	// In cases lie ngnix
    	String command = "docker search " + repo;
    	Result result = null;
    	try {
    	    result = ExecUtil.executeQuoted("/bin/sh", "-c", command);
    	} catch (ExecuteException e) {
    	    log.error("Error in executing command:" + command);
    	    throw new DirectorException("Error in executing command:" + command, e);
    	} catch (IOException e) {
    	    log.error("Error in executing command:" + command);
    	    throw new DirectorException("Error in executing command:" + command, e);
    	}
    	log.info("checkWithDockerSearch : Command executed for checking existence of repo and tab in hub : {}",
    		command);
    	int exitCode = -1;
    	exitCode = result.getExitCode();
    	if (exitCode != 0) {
    	    throw new ConnectionFailException("Unable to connect to docker hub");
    	}
    	String searchResult = result.getStdout();
    	log.info("searchResult = {}", searchResult);
    	String nl = System.getProperty("line.separator");
    	String[] split = searchResult.split(nl);
        int startIndex=0;
        
        if (split.length > 0) {
			if (StringUtils.isNotBlank(split[0]) && split[0].contains("NAME")
					&& split[0].contains("DESCRIPTION")
					&& split[0].contains("STARS")) {
				startIndex = 1;
			}

			for (; startIndex < split.length; startIndex++) {
				String string = split[startIndex];
				log.debug("Found the content: {}", string);
				if ("latest".equalsIgnoreCase(tag)) {
					String[] split2 = string.split(" ");
					if (split2.length > 0 && split2[0].equalsIgnoreCase(repo)) {
						repoTagExists = true;
					}
					break;
				} else {
					repoTagExists = string.contains(tag + "/");
					if (repoTagExists) {
						break;
					}
				}
			}
        } 	


    	return repoTagExists;

        }
    
    public static boolean checkWithV1Api(String repo, String tag)
    	    throws DirectorException, ConnectionFailException {

    	boolean repoTagExists = false;
    	log.info("Checking using docker search command");

    	/*
    	 * 
    	 * command =
    	 * "curl -i -X GET https://registry.hub.docker.com/v1/repositories/" +
    	 * repo + "/tags/" + tag; log.info(
    	 * "doesRepoTagExistInDockerHub, running command::" + command); try {
    	 * result = DirectorUtil.executeCommand("curl", "-i", "-X", "GET",
    	 * "https://registry.hub.docker.com/v1/repositories/" + repo + "/tags/"
    	 * + tag); } catch (ExecuteException e) { log.error(
    	 * "Error in executing command:" + command); throw new
    	 * DirectorException("Error in executing command:" + command, e); }
    	 * catch (IOException e) { log.error("Error in executing command:" +
    	 * command); throw new DirectorException("Error in executing command:" +
    	 * command, e); } String resultOutput = result.getStdout();
    	 * log.info("result:" + result + " console output::" + resultOutput); if
    	 * (StringUtils.isNotBlank(resultOutput)) { if (resultOutput.contains(
    	 * "200 OK")) { repoTagExists = true; } }
    	 */

    	String url = "https://registry.hub.docker.com/v1/repositories/" + repo + "/tags/" + tag;
    	HttpClient httpClient = HttpClientBuilder.create().build();
    	HttpGet get = new HttpGet(url);

    	try {
    	    HttpResponse execute = httpClient.execute(get);
    	    log.info("checkWithV1Api, URL for searhcing repo tag in docker hub: {}", url);
    	    log.info("checkWithV1Api, Response code: {}", execute.getStatusLine().getStatusCode());
    	    repoTagExists = (execute.getStatusLine().getStatusCode() == HttpStatus.SC_OK);

    	} catch (ClientProtocolException e) {
    	    log.error("Error in using http client to find repo tag existence in Docker hub: {}", url, e);
    	} catch (IOException e) {
    	    log.error("Error in using http client to find repo tag existence in Docker hub: {}", url, e);
    	}
    	return repoTagExists;

        }

    

}

@JsonIgnoreProperties(ignoreUnknown = true)
class RepoSearchResult {
    List<TagDetail> results;

    public List<TagDetail> getResults() {
        return results;
    }

    public void setResults(List<TagDetail> results) {
        this.results = results;
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
class TagDetail {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}