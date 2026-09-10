package helpers;

import static config.BrowserstackConfig.getAccessKey;
import static config.BrowserstackConfig.getUser;
import static io.restassured.RestAssured.given;


public class Browserstack {

    // curl -u "dmitry_8rmWIH:zW2u3gAFLNoZwF4qi874" -X GET "https://api.browserstack.com/app-automate/sessions/3eb4e7144fe10a55d34c0efbaae4b1c124bec2e0.json"
    // automation_session.video_url

    public static String videoUrl(String sessionId) {
        String url = String.format("https://api.browserstack.com/app-automate/sessions/%s.json", sessionId);

        return given()
            .auth().basic(getUser(), getAccessKey())
            .get(url)
            .then()
            .log().status()
            .log().body()
            .statusCode(200)
            .extract().path("automation_session.video_url");
    }
}
