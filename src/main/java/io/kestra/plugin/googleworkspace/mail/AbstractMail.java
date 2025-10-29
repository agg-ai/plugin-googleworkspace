package io.kestra.plugin.googleworkspace.mail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.runners.RunContext;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.plugin.googleworkspace.AbstractGoogleTask;
import java.io.IOException;
import java.security.GeneralSecurityException;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractMail extends AbstractGoogleTask {
        protected Gmail connection(RunContext runContext)
                        throws IllegalVariableEvaluationException, IOException, GeneralSecurityException {
                Credential credential = credential(runContext);

                return new Gmail.Builder(this.netHttpTransport(), JSON_FACTORY, credential)
                                .setApplicationName("Kestra")
                                .build();
        }
}
