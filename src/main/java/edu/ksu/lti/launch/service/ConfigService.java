package edu.ksu.lti.launch.service;

/**
 * This is an abstraction for configuration. It is used to look up key/value pairs of
 * configuration items from some config source. Most of our applications currently
 * use a simple database table but the config items could also come from an application
 * properties file or something similar.
 * Example config keys that might be requested from this service include things like:
 * <ul>
 * <li><code>canvas_url</code>: URL of the Canvas instance this application is bound to
 * <li><code>oauth_client_id</code>: application key used to request OAuth tokens from users
 * <li><code>oauth_client_secret</code>: application secret for requesting OAuth tokens from users
 * </ul>
 */
public interface ConfigService {
    String getConfigValue(String key);
}
