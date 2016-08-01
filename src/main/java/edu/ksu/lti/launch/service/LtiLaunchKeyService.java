package edu.ksu.lti.launch.service;

public interface LtiLaunchKeyService {
    String findSecretForKey(String key);
    String findOauthClientId();
    String findOauthClientSecret();
}
