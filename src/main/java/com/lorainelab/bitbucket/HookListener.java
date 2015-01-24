package com.lorainelab.bitbucket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.lorainelab.bitbucket.json.model.BitbucketPost;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Path("/")
public class HookListener {

    private static final Logger logger = LoggerFactory.getLogger(HookListener.class);
    private static final String CURRENT_DEV_BRANCH_NAME = "igb_8_4";
    private static final String MASTER_BRANCH = "master";
    private static final String BASE_HOOK_URL = "http://eos.transvar.org/jenkins/buildByToken/";
    private static final String DEVELOPMENT_BRANCH_JOB_NAME = "Development_Branch";
    private static final String MASTER_BRANCH_JOB_NAME = "Release_Branch";

    private final ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally

    @Path("/")
    @POST
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response processHook(@Context HttpServletRequest request, InputStream requestBody) {
        try {
            String urlEncodedPost = CharStreams.toString(new InputStreamReader(requestBody, "UTF-8"));
            String rawPostBodyContent = java.net.URLDecoder.decode(urlEncodedPost.substring(8), "UTF-8");
            BitbucketPost post = mapper.readValue(rawPostBodyContent, BitbucketPost.class);
            String branchName = post.getCommits().get(0).getBranch();

            switch (branchName) {
                case CURRENT_DEV_BRANCH_NAME:
                    logger.info("Triggering Development Branch Jenkins Build");
                    handleDevelopmentBranch();
                    break;
                case MASTER_BRANCH:
                    logger.info("Triggering Master Branch Jenkins Build");
                    handleMasterBranch();
            }
        } catch (IOException ex) {
            logger.error("error reading post from bitbucket");
        }
        return Response.ok().build();
    }

    private void handleDevelopmentBranch() {
        String url = BASE_HOOK_URL + "build?job=" + DEVELOPMENT_BRANCH_JOB_NAME + "&token=e0064f53b8c";
        sendGetRequest(url);
    }

    private void handleMasterBranch() {
        String url = BASE_HOOK_URL + "/buildWithParameters?job=" + MASTER_BRANCH_JOB_NAME + "&token=cz5tvwjo4k";
        sendGetRequest(url);
    }

    private void sendGetRequest(String url) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.getResponseCode();
            con.disconnect();
        } catch (IOException ex) {
            logger.error("could not trigger development branch build", ex);
        }
    }

}
