# LTI Launch

LTI Launch is a project designed to assist in the development of Java based LTI applications that work with the Canvas LMS. It provides functionality to authenticate the OAuth signature of an LTI launch request and handle the OAuth 2 user token exchange if needed for communicating with the Canvas API. After the launch request is verified, the user is forwarded to an initial view specified by the implementing application.

### Technologies Used
- Java 8
- Spring MVC 4.1.6
- Spring Security OAuth
- Google GSON
- Apache HTTP Client

### Usage
The LTI Launch project provides a number of interfaces that must be implemented in order for it to function in any given project, as well as a few beans that must be candidates for Spring Autowiring in the LTI application that wishes to use it. In order to begin this process, you will first need to add lti-launch as a Maven dependency.

After adding the project as a Maven dependency, you will need to implement each of the required Interfaces, which can be found in the `edu.ksu.lti.launch.service` package:
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

The following Bean must be made available for Autowiring by the LTI application:
- `canvasDomain` A string defining the primary canvasDomain that will be used for API calls and the OAuth exchange (most often, this should be the same as `canvas_url` above)

To utilize the LTI launch capabilities of this application after this point, you simply need to create a Spring Controller inside of your Spring application that extends `LtiLaunchController`, an abstract class that handles most of the LTI exchange. The LTI launch will happen by setting up the LTI application in Canvas to hit the URL `/launch` within your application.

The launch process sets up an `LtiSession` object in the HTTP session. The service class `LtiSessionService` can be autowired into your controller classes and used to get the LTI session. The LTI session holds all of the LTI launch data plus an `OauthToken` object that can be used for authentication when making calls to the Canvas API.

### Getting an OAuth token
In order to make calls to the Canvas API, your application must request an OAuth token from the user. Applications that need to talk to the Canvas API should call the `LtiLaunch.ensureApiTokenPresent()` method. This will attempt to retrieve an OAuth token for the user. If this fails, it will throw an `OauthTokenRequiredException`. Your application needs to catch this exception as well as the `InvalidOauthTokenException` and if these are encountered, redirect the user's browser to the relative URL `/beginOauth`. This will cause the `OauthController` to take over and have the user go through the OAuth 2 flow to request access to their account.

See also our [Canvas API Library](https://github.com/kstateome/canvas-api) project for more details on how to interact with the Canvas API from within your applications.
