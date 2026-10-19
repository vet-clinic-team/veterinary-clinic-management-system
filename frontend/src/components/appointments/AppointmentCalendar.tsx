
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";

import type {
  EventClickArg,
  EventDropArg,
  EventContentArg,
} from "@fullcalendar/core";

import { useNavigate } from "react-router-dom";

import type { Visit } from "../../types/visit";
import type { Pet } from "../../types/pet";
import type { Veterinarian } from "../../types/veterinarian";
import type { FollowUp } from "../../types/followUp";

type AppointmentCalendarProps = {
  appointments: Visit[];
  followUps: FollowUp[];
  pets: Pet[];
  veterinarians: Veterinarian[];

  onUpdate?: (
    appointment: Visit,
    newDate: string
  ) => void;

  onSelect?: (
    appointment: Visit
  ) => void;
};

function AppointmentCalendar({
  appointments,
  followUps,
  pets,
  veterinarians,
  onUpdate,
  onSelect,
}: AppointmentCalendarProps) {
  const navigate = useNavigate();

  // Existing appointment events.
  const appointmentEvents = appointments.map(
    (appointment) => {
      const pet =
        pets.find(
          (p) => p.id === appointment.petId
        )?.name ?? `Pet #${appointment.petId}`;

      const vet =
        veterinarians.find(
          (v) => v.id === appointment.vetId
        )?.name ?? `Vet #${appointment.vetId}`;

      return {
        id: `appointment-${appointment.id}`,

        title: pet,

        start: appointment.scheduledAt,

        end: new Date(
          new Date(
            appointment.scheduledAt
          ).getTime() +
            30 * 60 * 1000
        ),

        extendedProps: {
          appointment,
          pet,
          vet,
        },

        classNames: [
          appointment.status === "COMPLETED"
            ? "event-completed"
            : appointment.status === "CANCELLED"
            ? "event-cancelled"
            : appointment.status === "IN_EXAM"
            ? "event-exam"
            : "event-scheduled",
        ],
      };
    }
  );

  // Pending follow-up events.
  const followUpEvents = followUps
    .filter(
      (followUp) =>
        followUp.status === "PENDING"
    )
    .map((followUp) => {
      const pet =
        pets.find(
          (p) => p.id === followUp.petId
        )?.name ?? `Pet #${followUp.petId}`;

      const procedureName =
        followUp.procedureType.replace(
          /_/g,
          " "
        );

      return {
        id: `follow-up-${followUp.id}`,

        title: `${pet} - ${procedureName}`,

        start: followUp.dueDate,

        allDay: true,

        editable: false,

        classNames: ["event-follow-up"],

        extendedProps: {
          followUp,
          pet,
        },
      };
    });

  // Display appointments and follow-ups together.
  const events = [
    ...appointmentEvents,
    ...followUpEvents,
  ];

  const handleEventDrop = (
    info: EventDropArg
  ) => {
    const appointment = info.event
      .extendedProps.appointment as
      | Visit
      | undefined;

    // Follow-ups cannot be moved by dragging.
    if (!appointment || !onUpdate) {
      info.revert();
      return;
    }

    if (!info.event.start) {
      info.revert();
      return;
    }

    const newDate = info.event.start
      .toLocaleString("sv-SE")
      .replace(" ", "T");

    onUpdate(
      appointment,
      newDate
    );
  };

  const handleEventClick = (
    info: EventClickArg
  ) => {
    const followUp = info.event
      .extendedProps.followUp as
      | FollowUp
      | undefined;

    // Open Pet Detail for follow-up events.
    if (followUp) {
      navigate(
        `/pets/${followUp.petId}`
      );
      return;
    }

    // Keep existing appointment click behavior.
    const appointment = info.event
      .extendedProps.appointment as
      | Visit
      | undefined;

    if (appointment && onSelect) {
      onSelect(appointment);
    }
  };

  return (
    <div
      className="
        rounded-xl
        border
        border-slate-200
        bg-white
        p-6
      "
    >
      <h2
        className="
          mb-5
          text-xl
          font-semibold
          text-slate-900
        "
      >
        Appointment Calendar
      </h2>

      <FullCalendar
        plugins={[
          dayGridPlugin,
          timeGridPlugin,
          interactionPlugin,
        ]}
        initialView="dayGridMonth"
        headerToolbar={{
          left: "prev,next today",
          center: "title",
          right:
            "dayGridMonth,timeGridWeek,timeGridDay",
        }}
        events={events}
        editable
        eventDrop={handleEventDrop}
        eventClick={handleEventClick}
        height="620px"
        dayMaxEvents={3}
        slotMinTime="08:00:00"
        slotMaxTime="20:00:00"
        slotDuration="00:30:00"
        eventTimeFormat={{
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
        }}
        eventContent={(
          eventInfo: EventContentArg
        ) => {
          const followUp = eventInfo.event
            .extendedProps.followUp as
            | FollowUp
            | undefined;

          const pet = eventInfo.event
            .extendedProps.pet as
            | string
            | undefined;

          const procedureName =
            followUp?.procedureType.replace(
              /_/g,
              " "
            );

          return (
            <div className="calendar-event min-w-0 w-full">
              {followUp ? (
                <>
                  <div
                    className="
                      font-semibold
                      whitespace-normal
                      break-words
                      leading-tight
                    "
                  >
                    {pet}
                  </div>

                  <div
                    className="
                      mt-1
                      text-xs
                      whitespace-normal
                      break-words
                      leading-tight
                      opacity-90
                    "
                  >
                    {procedureName}
                  </div>
                </>
              ) : (
                <>
                  <div className="calendar-time">
                    {eventInfo.timeText}
                  </div>

                  <div className="calendar-title">
                    {eventInfo.event.title}
                  </div>

                  <div className="calendar-vet">
                    Dr.{" "}
                    {
                      eventInfo.event
                        .extendedProps.vet
                    }
                  </div>
                </>
              )}
            </div>
          );
        }}
      />
    </div>
  );
}

export default AppointmentCalendar;

