package edu.ksu.lti.launch.service;

public interface OauthTokenService {
    String getOauthToken(String userId);
    String createOauthToken(String userId);
    String updateToken(String userId, String accessToken);
}
