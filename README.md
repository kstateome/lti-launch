# lti-launch

LTI Launch is a project designed to assist in the development of LTI applications that work with the Canvas LMS. It provides utilities for handling the OAuth 2.0 exchanges, API token management, as well as an abstract controller that will provide the basic entry point for any LTI application.

### Technologies Used
- Spring MVC 4.1.6
- Spring Security OAuth
- Java 8
- Google GSON
- Apache HTTP Client

### Usage
The LTI Launch project provides a number of interfaces that must be implemented in order for it to function in any given project, as well as a few beans that must be candidates for Spring Autowiring in the LTI application that wishes to use it. In order to begin this process, you will first need to add lti-launch as a Maven dependency.

After adding the project as a Maven dependency, you will need to implement each of the required Interfaces, which can be found in the `edu.ksu.lti.launch.service` package:
- ConfigService
    - This interface must be able to supply each of the following config keys:
        - `canvas_url` - The first valid base canvas url this instance can talk to (e.g. "https://canvas.k-state.edu")
        - `canvas_url_2` - The second base canvas url this instance can talk to. It can be blank if there is no second canvas url.
        - `oauth_client_id` - The OAuth Client ID for the application
        - `oauth_client_secret` - The OAuth Client Secret for this application
- LtiLaunchKeyService
- OauthTokenService

The following Bean must be made available for Autowiring by the LTI application:
- `canvasDomain` A string defining the primary canvasDomain that will be used for API calls and the OAuth exchange (most often, this should be the same as `canvas_url` above)

To utilize the LTI launch capabilities of this application after this point, you simply need to create a Spring Controller inside of your Spring application that extends `LtiLaunchController`, an abstract class that handles most of the LTI exchange.

To get an API token from the LTI launch project, you will need to wire in an instance of the `LtiSessionService` class, which provides the ability to access the LtiSession attributes. One of these attributes is `canvasOauthToken`, an object designed to handle the API token. You may either call the `getToken` method on this object, or use the convenience method in the LtiSession: `getApiToken`.
