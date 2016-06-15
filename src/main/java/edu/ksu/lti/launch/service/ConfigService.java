package edu.ksu.lti.launch.service;

/*
 * This is an abstraction for configuration. Currently lti applications use the database as a repository.
 * However we want the teval rewrite to use property files. In the future, hopefully all of the  lti applications
 * will not be using the database for configuration.
 */
public interface ConfigService {
    String getConfigValue(String key);
}
