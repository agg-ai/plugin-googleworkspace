package io.kestra.plugin.googleworkspace.calendar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(examples = @Example(full = true, code = """
        id: googleworkspace_calendar_get_event
        namespace: company.team

        tasks:
          - id: get_event
            type: io.kestra.plugin.googleworkspace.calendar.GetEvent
            calendarId: primary
            eventId: "abcdef123456"
            maxAttendees: 50
        """))
@Schema(title = "Fetch a Google Calendar event by ID.")
public class GetEvent extends AbstractCalendar implements RunnableTask<GetEvent.Output> {
    @Schema(title = "Calendar ID (e.g., 'primary' or a calendar email)", description = "Calendar identifier. To retrieve calendar IDs call the calendarList.list method. If you want to access the primary calendar of the currently logged in user, use the \"primary\" keyword.")
    @NotNull
    protected Property<String> calendarId;

    @Schema(title = "Event ID", description = "Event identifier.")
    @NotNull
    protected Property<String> eventId;

    @Schema(title = "Upper bound on the number of attendees to include", description = "The maximum number of attendees to include in the response. If there are more than the specified number of attendees, only the participant is returned. Optional.")
    protected Property<Integer> maxAttendees;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Calendar service = this.connection(runContext);
        Logger logger = runContext.logger();

        String rCalendarId = runContext.render(calendarId).as(String.class).orElseThrow();
        String rEventId = runContext.render(eventId).as(String.class).orElseThrow();
        Integer rMaxAttendees = runContext.render(maxAttendees).as(Integer.class).orElse(null);

        var req = service.events().get(rCalendarId, rEventId);
        if (rMaxAttendees != null)
            req.setMaxAttendees(rMaxAttendees);

        Event googleEvent = req.execute();

        Map<String, Object> eventMetadata = JacksonMapper.ofJson().convertValue(
                googleEvent, new TypeReference<Map<String, Object>>() {
                });

        logger.debug("fetched event '{}' from calendar '{}'", rEventId, rCalendarId);

        return Output.builder()
                .event(io.kestra.plugin.googleworkspace.calendar.models.Event.of(googleEvent))
                .metadata(eventMetadata)
                .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Full Google Calendar event resource (wrapped)")
        private final io.kestra.plugin.googleworkspace.calendar.models.Event event;

        @Schema(title = "Complete metadata of Google Calendar event resource")
        private final Map<String, Object> metadata;
    }
}
