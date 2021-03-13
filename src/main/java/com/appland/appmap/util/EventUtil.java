package com.appland.appmap.util;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.transform.annotations.MethodEvent;

public class EventUtil {

    /**
     *
     * @param event
     * @return true is the event is of type return
     */
    public static boolean isReturnEvent(Event event) {
        return event.event.equalsIgnoreCase(MethodEvent.METHOD_RETURN.getEventString());
    }

    /**
     * Removes unnecessary or duplicated fields depending on the event type
     */
    public static void removeUnnecessaryInfoForReturnEvents(Event event) {
        if (event != null && event.event != null && isReturnEvent(event)) {
            if(event.path!=null) event.path = null;
            if(event.lineNumber!=null) event.lineNumber = null;
            if(event.isStatic != null) event.isStatic = null;
            if(event.definedClass!=null) event.definedClass= null;
            if(event.methodId != null) event.methodId = null;
        }
    }
}
