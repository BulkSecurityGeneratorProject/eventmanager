package com.sgebs.eventmanager.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.sgebs.eventmanager.domain.Event;
import com.sgebs.eventmanager.domain.User;
import com.sgebs.eventmanager.repository.UserRepository;
import com.sgebs.eventmanager.service.EventService;
import com.sgebs.eventmanager.service.UserService;
import com.sgebs.eventmanager.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Event.
 */
@RestController
@RequestMapping("/api")
public class EventResource {

    private final Logger log = LoggerFactory.getLogger(EventResource.class);

    @Inject
    private EventService eventService;

    @Inject
    private UserService userService;


    private List<Event> allEvents;

    /**
     * POST  /events : Create a new event.
     *
     * @param event the event to create
     * @return the ResponseEntity with status 201 (Created) and with body the new event, or with status 400 (Bad Request) if the event has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/events",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) throws URISyntaxException {
        log.debug("REST request to save Event : {}", event);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();
        event.setCreatedBy(userService.getUserByLogin(login));
        ZonedDateTime startDate =  event.getDate();
        ZonedDateTime endDate  = event.getEndDate();

        allEvents = eventService.findAll();
        for(Event currentEvent:allEvents){
            ZonedDateTime currentStartDate =  currentEvent.getDate();
            ZonedDateTime currentEndDate = currentEvent.getEndDate();
            if(currentEvent.getLocation().getId() == event.getLocation().getId()
                &&
//                ((startDate.isAfter(currentStartDate) && startDate.isBefore(currentEndDate) ||
//                    (endDate.isAfter(currentStartDate) && endDate.isBefore(currentEndDate)) ||
//                    (startDate.isBefore(currentStartDate) && endDate.isAfter(currentEndDate))))
                event.isIntersect(currentEvent)
                ){
                return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("event", "locationbusy", "The current location is busy between selected range.")).body(null);

            }

        }


        log.debug(auth.toString());
//        event.setCreatedBy();
        if (event.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("event", "idexists", "A new event cannot already have an ID")).body(null);
        }
        Event result = eventService.save(event);
        return ResponseEntity.created(new URI("/api/events/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("event", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /events : Updates an existing event.
     *
     * @param event the event to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated event,
     * or with status 400 (Bad Request) if the event is not valid,
     * or with status 500 (Internal Server Error) if the event couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/events",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Event> updateEvent(@Valid @RequestBody Event event) throws URISyntaxException {
        log.debug("REST request to update Event : {}", event);
        if (event.getId() == null) {
            return createEvent(event);
        }
        Event result = eventService.save(event);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("event", event.getId().toString()))
            .body(result);
    }

    /**
     * GET  /events : get all the events.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of events in body
     */
    @RequestMapping(value = "/events",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Event> getAllEvents() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("REST request to get all Events for current logged in user [{}], ", auth.getName());
        return eventService.findByCreatedByLogin(auth.getName());
    }

    /**
     * GET  /events/:id : get the "id" event.
     *
     * @param id the id of the event to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the event, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/events/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Event> getEvent(@PathVariable Long id) {
        log.debug("REST request to get Event : {}", id);
        Event event = eventService.findOne(id);
        return Optional.ofNullable(event)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /events/:id : delete the "id" event.
     *
     * @param id the id of the event to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/events/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        log.debug("REST request to delete Event : {}", id);
        eventService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("event", id.toString())).build();
    }

}
