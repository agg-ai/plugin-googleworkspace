package io.kestra.plugin.googleworkspace.calendar;

import com.google.api.services.calendar.Calendar;
import com.google.api.client.auth.oauth2.Credential;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.googleworkspace.AbstractGoogleTask;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractCalendar extends AbstractGoogleTask {
    protected Calendar connection(RunContext runContext)
            throws IllegalVariableEvaluationException, IOException, GeneralSecurityException {
        Credential credential = credential(runContext);

        return new Calendar.Builder(this.netHttpTransport(), JSON_FACTORY, credential)
                .setApplicationName("Kestra")
                .build();
    }
}
