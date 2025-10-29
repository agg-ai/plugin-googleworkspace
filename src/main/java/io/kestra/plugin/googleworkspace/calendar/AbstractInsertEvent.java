package io.kestra.plugin.googleworkspace.calendar;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.googleworkspace.helpers.PropertyHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Event.Creator;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractInsertEvent extends AbstractCalendar {

    @Schema(title = "Calendar ID (e.g., 'primary' or a calendar email)", description = "Calendar identifier. To retrieve calendar IDs call the calendarList.list method. If you want to access the primary calendar of the currently logged in user, use the \"primary\" keyword.")
    @NotNull
    protected Property<String> calendarId;

    @Schema(title = "Title of the event")
    @NotNull
    protected Property<String> summary;

    @Schema(title = "Description of the event")
    @PluginProperty(dynamic = true)
    protected Property<String> eventDescription;

    @Schema(title = "Geographic location of the event as free-form text")
    protected Property<String> location;

    @Schema(title = "Start time of the event")
    @NotNull
    @PluginProperty
    protected Property<CalendarTime> startTime;

    @Schema(title = "End time of the event")
    @NotNull
    @PluginProperty
    protected Property<CalendarTime> endTime;

    @Schema(title = "Creator of the event")
    @PluginProperty
    protected Property<Attendee> creator;

    @Schema(title = "List of attendees in the event")
    protected Property<List<Attendee>> attendees;

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.extern.jackson.Jacksonized
    public static class CalendarTime {
        @Schema(title = "Time of the event in the ISO 8601 Datetime format, for example, `2024-11-28T09:00:00-07:00`")
        protected String dateTime;

        @Schema(title = "Timezone associated with the dateTime, for example, `America/Los_Angeles`")
        protected String timeZone;
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.extern.jackson.Jacksonized
    public static class Attendee {
        @Schema(title = "Display name of the attendee")
        protected String displayName;

        @Schema(title = "Email of the attendee")
        protected String email;
    }

    protected Event event(RunContext runContext) throws IllegalVariableEvaluationException {
        Event eventMetadata = new Event();

        var renderedSummary = runContext.render(this.summary).as(String.class).orElseThrow();
        eventMetadata.setSummary(renderedSummary);

        var renderedDescription = PropertyHelper.safeRender(runContext, this.eventDescription, null, String.class);
        if (renderedDescription != null && !renderedDescription.isEmpty()) {
            eventMetadata.setDescription(renderedDescription);
        }

        var renderedLocation = PropertyHelper.safeRender(runContext, this.location, null, String.class);
        if (renderedLocation != null && !renderedLocation.isEmpty()) {
            eventMetadata.setLocation(renderedLocation);
        }

        CalendarTime renderedStartTime = PropertyHelper.safeRender(runContext, this.startTime, null, CalendarTime.class,
                true);
        if (renderedStartTime == null) {
            throw new IllegalVariableEvaluationException("Failed to render startTime property");
        }

        CalendarTime renderedEndTime = PropertyHelper.safeRender(runContext, this.endTime, null, CalendarTime.class,
                true);
        if (renderedEndTime == null) {
            throw new IllegalVariableEvaluationException("Failed to render endTime property");
        }

        EventDateTime eventStartTime = new EventDateTime()
                .setDateTime(new DateTime(renderedStartTime.dateTime))
                .setTimeZone(renderedStartTime.timeZone);
        eventMetadata.setStart(eventStartTime);

        EventDateTime eventEndTime = new EventDateTime()
                .setDateTime(new DateTime(renderedEndTime.dateTime))
                .setTimeZone(renderedEndTime.timeZone);
        eventMetadata.setEnd(eventEndTime);

        var renderedAttendees = PropertyHelper.safeRenderList(runContext, attendees, new ArrayList<>(), Attendee.class,
                true);
        if (renderedAttendees != null && !renderedAttendees.isEmpty()) {
            List<EventAttendee> eventAttendees = new ArrayList<>();
            for (Attendee attendee : renderedAttendees) {
                eventAttendees.add(new EventAttendee()
                        .setDisplayName(attendee.displayName)
                        .setEmail(attendee.email));
            }
            eventMetadata.setAttendees(eventAttendees);
        }

        var renderedCreator = PropertyHelper.safeRender(runContext, creator, null, Attendee.class, true);
        if (renderedCreator != null) {
            eventMetadata.setCreator(new Creator()
                    .setDisplayName(renderedCreator.displayName)
                    .setEmail(renderedCreator.email));
        }

        return eventMetadata;
    }
}
