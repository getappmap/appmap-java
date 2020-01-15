/**
 * Used by the AppMap agent to instrument application code.
 * As classes are loaded by the JVM, they are matched according to the configuration settings in <code>appmap.yml</code>.
 * When a method matches the configuration, it is modified to notify com.appland.appmap.process on each invocation.
 * Some methods also trigger special behavior, such as causing appmap.json data to be written when the method returns.
 */
package com.appland.appmap.transform;
