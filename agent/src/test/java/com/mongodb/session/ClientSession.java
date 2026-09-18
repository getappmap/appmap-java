package com.mongodb.session;

/**
 * Stand-in for the driver's session interface, so that the Mongo hook's
 * argument handling can be tested without the driver on the class path. The
 * hook recognizes sessions by this type name.
 */
public interface ClientSession {
}
