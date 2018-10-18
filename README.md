# LTI Launch
 
LTI Launch is a project designed to assist in the development of Java based LTI applications that work with the Canvas LMS. It provides functionality to authenticate the OAuth signature of an LTI launch request and handle the OAuth 2 user token exchange if needed for communicating with the Canvas API. After the launch request is verified, the user is forwarded to an initial view specified by the implementing application.

### Technologies Used
- Java 8
- Maven (Compatible with 3.5.2, requires 3.1+)
- Spring MVC 4.1.6
- Spring Security OAuth
- Google GSON
- Apache HTTP Client

### Set Up
The LTI Launch project provides a number of interfaces that must be implemented in order for it to function in any given project, as well as a few beans that must be candidates for Spring Autowiring in the LTI application that wishes to use it. As an example you can look at [a trivial LTI application](https://github.com/kstateome/lti-launch-example-webapp) which has the minimal setup for an LTI application launch. For a more complicated application you can also look at our [attendance taking LTI application](https://github.com/kstateome/lti-attendance)

After adding this project as a Maven dependency, you will need to implement each of the required Interfaces, which can be found in the `edu.ksu.lti.launch.service` package:
- ConfigService
    - This is a simple key/value lookup service for retrieving configuration items that the applicatin needs. It must provide values for the following keys:
        - `canvas_url` - The first valid base canvas URL this instance can talk to (e.g. https://k-state.instructure.com)
        - `canvas_url_2` - The second base canvas URL this instance can talk to. For example if you have a vanity URL like https://canvas.k-state.edu. It can be blank if there is no second canvas URL.
        - `oauth_client_id` - The OAuth Client ID for the application
        - `oauth_client_secret` - The OAuth Client Secret for this application
- LtiLaunchKeyService
    - A service that is able to take an application launch key and return the associated shared secret.
- OauthTokenService
    - A service that can handle the persisting and retrieving of user OAuth refresh tokens

The implementations of these interfaces need to be set up as Spring beans so that they can be autowired into your application. In addition, there also needs to be a `canvasDomain` bean which is simply a string defining the primary Canvas domain to be used for API calls and OAuth purposes. Usually this will be the same as `canvas_url` above.

### Usage
Now that the application is configured, create a Spring controller which extends `LtiLaunchController`. This is an abstract class that provides a method which handles the initial LTI launch request, sets up some user information in a new session and then forwards the user to an initial page which you define by implementing the `getInitialViewPath()` method. The initial launch method in `LtiLaunchController` is mapped to the URL `/launch` within the application context. 

The launch process sets up an `LtiSession` object in the HTTP session. The service class `LtiSessionService` can be autowired into your controller classes and used to get the LTI session. The LTI session holds all of the LTI launch data that comes in the launch POST request. This includes information about the user who launched the application and the context from which it was launched. If your application needs to interact with the Canvas API, the session can also hold an `OauthToken` object that can be used for authentication when making calls to the Canvas API.

### Getting an OAuth token for calls to the Canvas API
As mentioned above, in order to make calls to the Canvas API, your application must request an OAuth token from the user. The process is documented in the Canvas API documentation [here](https://canvas.instructure.com/doc/api/file.oauth.html). Within this code, applications that need to talk to the Canvas API should call the `LtiLaunch.ensureApiTokenPresent()` method at some point while they are preparing to display the initial page of the application to the user. This will attempt to retrieve an OAuth token for the user from your `OauthTokenService` implementation. If this fails it will throw an `OauthTokenRequiredException`. If the token exists but is found to be invalid due to token expiration or the user having revoked the token manually, an `InvalidOauthTokenException` will be thrown. Your application needs to catch both of these exceptions and if they are thrown, redirect the user's browser to the relative URL `/beginOauth`. This will cause the `OauthController` class to take over. It will direct the user through the OAuth 2 flow to request an access token from the user. When a token is granted by the user it will be stored to your `OauthTokenService` implementation and then be available for use in API calls.

See also [Canvas API Library](https://github.com/kstateome/canvas-api) project for more details on how to interact with the Canvas API from within your applications.

### License
This software is licensed under the LGPL v3 license. Please see the [License.txt file](License.txt) in this repository for license details.
