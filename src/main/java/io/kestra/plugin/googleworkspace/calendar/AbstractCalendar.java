package io.kestra.plugin.googleworkspace.calendar;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.googleworkspace.AbstractTask;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractCalendar extends AbstractTask {
    @Builder.Default
    protected Property<List<String>> scopes = Property.ofValue(List.of(CalendarScopes.CALENDAR));

    protected Calendar connection(RunContext runContext)
            throws IllegalVariableEvaluationException, IOException, GeneralSecurityException {
        String renderedAccessToken = runContext.render(this.accessToken).as(String.class).orElseThrow();

        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod());
        credential.setAccessToken(renderedAccessToken);

        return new Calendar.Builder(this.netHttpTransport(), JSON_FACTORY, credential)
                .setApplicationName("Kestra")
                .build();
    }
}
