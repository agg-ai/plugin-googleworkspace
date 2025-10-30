package io.kestra.plugin.googleworkspace.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.googleworkspace.calendar.AbstractInsertEvent.Attendee;
import io.kestra.plugin.googleworkspace.helpers.PropertyHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(examples = @Example(full = true, code = """
          id: googleworkspace_calendar_update_event
          namespace: company.team

          tasks:
            - id: update_event
              type: io.kestra.plugin.googleworkspace.calendar.UpdateEvent
              calendarId: primary
              eventId: "abcdef123456"
              patch: true
              sendUpdates: externalOnly
              summary: "Weekly standup (rescheduled)"
              startTime:
                dateTime: "2025-08-12T10:00:00+05:30"
                timeZone: "Asia/Kolkata"
              endTime:
                dateTime: "2025-08-12T10:30:00+05:30"
                timeZone: "Asia/Kolkata"
              attendees:
                - email: a@example.com
                - email: team@example.com
        """))
@Schema(title = "Update a Google Calendar event.")
public class UpdateEvent extends AbstractCalendar implements RunnableTask<UpdateEvent.Output> {
    @Schema(title = "Calendar ID")
    @NotNull
    protected Property<String> calendarId;

    @Schema(title = "Event ID")
    @NotNull
    protected Property<String> eventId;

    @Schema(title = "Use PATCH (partial) when true, or UPDATE (replace) when false.")
    @Builder.Default
    protected Property<Boolean> patch = Property.ofValue(true);

    @Schema(title = "Send update emails (default: none)", description = "Guests who should receive notifications about the deletion of the event.", allowableValues = {
            "all", "none", "externalOnly" })
    @Builder.Default
    protected Property<String> sendUpdates = Property.ofValue("none");

    @Schema(title = "Title")
    protected Property<String> summary;

    @Schema(title = "Description of the event")
    protected Property<String> eventDescription;

    @Schema(title = "Location (free-form)")
    protected Property<String> location;

    @Schema(title = "Start time of the event")
    @NotNull
    @PluginProperty
    protected Property<AbstractInsertEvent.CalendarTime> startTime;

    @Schema(title = "End time of the event")
    @NotNull
    @PluginProperty
    protected Property<AbstractInsertEvent.CalendarTime> endTime;

    @Schema(title = "List of attendees in the event")
    protected Property<List<AbstractInsertEvent.Attendee>> attendees;

    @Schema(title = "Event status (default: confirmed)", description = "The status of the event.", allowableValues = {
            "confirmed", "tentative", "cancelled" })
    @Builder.Default
    protected Property<String> status = Property.ofValue("confirmed");

    @Override
    public Output run(RunContext runContext) throws Exception {
        Calendar service = this.connection(runContext);
        Logger logger = runContext.logger();

        String rCalendarId = runContext.render(calendarId).as(String.class).orElseThrow();
        String rEventId = runContext.render(eventId).as(String.class).orElseThrow();
        Boolean renderedPatch = PropertyHelper.safeRender(runContext, patch, true, Boolean.class);
        String renderedSendUpdates = PropertyHelper.safeRender(runContext, sendUpdates, "none", String.class);
        String renderedSummary = PropertyHelper.safeRender(runContext, summary, null, String.class);
        String renderedDescription = PropertyHelper.safeRender(runContext, eventDescription, null,
                String.class);
        String renderedLocation = PropertyHelper.safeRender(runContext, location, null, String.class);
        AbstractInsertEvent.CalendarTime renderedStartTime = PropertyHelper.safeRender(runContext, startTime,
                null, AbstractInsertEvent.CalendarTime.class, true);
        AbstractInsertEvent.CalendarTime renderedEndTime = PropertyHelper.safeRender(runContext, endTime, null,
                AbstractInsertEvent.CalendarTime.class, true);

        var renderedAttendees = PropertyHelper.safeRenderList(runContext, attendees, new ArrayList<>(), Attendee.class,
                true);

        String renderedStatus = PropertyHelper.safeRender(runContext, status, null, String.class);

        Event rBody = new Event();

        if (renderedSummary != null && !renderedSummary.isEmpty()) {
            rBody.setSummary(renderedSummary);
        }

        if (renderedDescription != null && !renderedDescription.isEmpty()) {
            rBody.setDescription(renderedDescription);
        }

        if (renderedLocation != null && !renderedLocation.isEmpty()) {
            rBody.setLocation(renderedLocation);
        }

        if (renderedStartTime != null) {
            rBody.setStart(new EventDateTime()
                    .setDateTime(new DateTime(renderedStartTime.dateTime))
                    .setTimeZone(renderedStartTime.timeZone));
        }

        if (renderedEndTime != null) {
            rBody.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(renderedEndTime.dateTime))
                    .setTimeZone(renderedEndTime.timeZone));
        }

        if (renderedAttendees != null && !renderedAttendees.isEmpty()) {
            List<EventAttendee> eventAttendees = new ArrayList<>();
            for (Attendee attendee : renderedAttendees) {
                eventAttendees.add(new EventAttendee()
                        .setDisplayName(attendee.displayName)
                        .setEmail(attendee.email));
            }

            if (!eventAttendees.isEmpty()) {
                rBody.setAttendees(eventAttendees);
            }
        }

        if (renderedStatus != null && !renderedStatus.isEmpty()) {
            rBody.setStatus(renderedStatus);
        }

        renderedSendUpdates = renderedSendUpdates.isEmpty() ? "none" : renderedSendUpdates;

        Event updatedEvent = renderedPatch
                ? service.events().patch(rCalendarId, rEventId,
                        rBody).setSendUpdates(renderedSendUpdates).execute()
                : service.events().update(rCalendarId, rEventId,
                        rBody).setSendUpdates(renderedSendUpdates).execute();

        logger.debug("{} event '{}' in calendar '{}'", renderedPatch ? "Patched" : "Updated", rEventId, rCalendarId);

        return Output.builder()
                .event(io.kestra.plugin.googleworkspace.calendar.models.Event.of(updatedEvent))
                .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        private final io.kestra.plugin.googleworkspace.calendar.models.Event event;
    }
}
