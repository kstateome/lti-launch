package edu.ksu.lti.launch.model;

public class LtiLaunchKeyAndSecret {
    private String key;
    private String secret;
    private String consumerProfile;

    public LtiLaunchKeyAndSecret(String key, String secret, String consumerProfile) {
        this.key = key;
        this.secret = secret;
        this.consumerProfile = consumerProfile;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public String getConsumerProfile() {
        return consumerProfile;
    }
}
