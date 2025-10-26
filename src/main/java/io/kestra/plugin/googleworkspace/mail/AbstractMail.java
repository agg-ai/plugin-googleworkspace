package io.kestra.plugin.googleworkspace.mail;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.plugin.googleworkspace.AbstractTask;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractMail extends AbstractTask {
    @Builder.Default
    protected Property<List<String>> scopes = Property.ofValue(List.of(
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND));

    protected Gmail connection(RunContext runContext)
            throws IllegalVariableEvaluationException, IOException, GeneralSecurityException {
        String renderedAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
        credential.setAccessToken(renderedAccessToken);

        return new Gmail.Builder(this.netHttpTransport(), JSON_FACTORY, credential)
                .setApplicationName("Kestra")
                .build();
    }
}
