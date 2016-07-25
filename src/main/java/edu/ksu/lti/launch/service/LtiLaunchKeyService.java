package edu.ksu.lti.launch.service;

public interface LtiLaunchKeyService {
    String findSecretForKey(String key);
    String getClientId();
    String getClientSecret();
}
