package org.elearning.backend.security.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;


@Component
public class Listener{

    private static final Logger logger = LoggerFactory.getLogger(Listener.class);


    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failures) {
        logger.info("Authentication failed! user={}, timestamp={}",
                failures.getAuthentication().getName(),
                failures.getTimestamp()
        );
    }
}
