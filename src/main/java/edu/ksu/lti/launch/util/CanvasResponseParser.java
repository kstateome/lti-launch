package edu.ksu.lti.launch.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CanvasResponseParser {
    public String parseToken(HttpResponse httpResponse) throws IOException {
        JsonObject responseContent = new Gson().fromJson(EntityUtils.toString(httpResponse.getEntity()), JsonObject.class);
        return responseContent.get("access_token").getAsString();
    }
}
