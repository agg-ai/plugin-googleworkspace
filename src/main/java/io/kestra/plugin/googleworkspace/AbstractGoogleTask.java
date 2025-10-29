package io.kestra.plugin.googleworkspace;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.io.IOException;
import java.security.GeneralSecurityException;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractGoogleTask extends Task {
    protected static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    protected Property<String> accessToken;

    protected Credential credential(RunContext runContext) throws IllegalVariableEvaluationException, IOException {
        String renderedAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
        credential.setAccessToken(renderedAccessToken);

        return credential;
    }

    protected NetHttpTransport netHttpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }
}
