package com.appland.appmap.process.hooks;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * Regression test for a {@link NoClassDefFoundError} involving {@link java.sql.SQLException}.
 * <p>
 * In certain environments (e.g., specific configurations of Oracle UCP or custom container classloaders),
 * {@code java.sql.SQLException} might not be visible to the classloader responsible for loading
 * {@code com.appland.appmap.process.hooks.SqlQuery}. This can lead to a crash when the agent attempts
 * to handle SQL events.
 * <p>
 * The crash manifests as:
 * <pre>
 * Caused by: com.example.operation.flow.FlowException: java/sql/SQLException
 * ...
 * Caused by: java.lang.NoClassDefFoundError: java/sql/SQLException
 *     at com.appland.appmap.process.hooks.SqlQuery.getDbName(SqlQuery.java:76)
 *     at com.appland.appmap.process.hooks.SqlQuery.recordSql(SqlQuery.java:89)
 *     at com.appland.appmap.process.hooks.SqlQuery.executeQuery(SqlQuery.java:172)
 * </pre>
 * <p>
 * This test reproduces the environment by using a custom {@link ClassLoader} that explicitly
 * throws {@link ClassNotFoundException} when {@code java.sql.SQLException} is requested.
 * It verifies that {@code SqlQuery} can be loaded and executed without triggering the error.
 */
public class SqlQuerySQLExceptionAvailabilityTest {

    @Test
    public void testSqlQueryResilienceToMissingSQLException() throws Exception {
        // 1. Create a RestrictedClassLoader that hides java.sql.SQLException
        ClassLoader restrictedLoader = new RestrictedClassLoader(this.getClass().getClassLoader());

        // 2. Load the SqlQuery class using the restricted loader.
        // This forces the verifier to check dependencies of SqlQuery using our restricted loader.
        // If SqlQuery explicitly catches or references SQLException in a way that requires resolution,
        // this (or the method invocation below) should fail.
        String sqlQueryClassName = "com.appland.appmap.process.hooks.SqlQuery";
        Class<?> sqlQueryClass = restrictedLoader.loadClass(sqlQueryClassName);

        // 3. Invoke a method that triggers the problematic code path (getDbName).
        // We choose recordSql(Event, Connection, String) which calls getDbName(Connection).
        Method recordSqlMethod = sqlQueryClass.getMethod("recordSql",
            com.appland.appmap.output.v1.Event.class,
            java.sql.Statement.class,
            String.class
        );

        // Prepare arguments
        com.appland.appmap.output.v1.Event mockEvent = mock(com.appland.appmap.output.v1.Event.class);
        java.sql.Statement mockStatement = mock(java.sql.Statement.class);

        assertDoesNotThrow(() -> {
            recordSqlMethod.invoke(null, mockEvent, mockStatement, "SELECT 1");
        }, "SqlQuery should not fail even if java.sql.SQLException is missing");
    }

    /**
     * A ClassLoader that throws ClassNotFoundException for java.sql.SQLException
     * and forces re-definition of SqlQuery to ensure it's loaded by this loader.
     */
    private static class RestrictedClassLoader extends ClassLoader {

        public RestrictedClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            String forbiddenClassName = "java.sql.SQLException";
            if (forbiddenClassName.equals(name)) {
                throw new ClassNotFoundException("Simulated missing class: " + name);
            }

            // If it's the target class, we want to define it ourselves to ensure
            // this classloader (and its restrictions) is used for verification.
            String targetClassName = "com.appland.appmap.process.hooks.SqlQuery";
            if (targetClassName.equals(name)) {
                // Check if already loaded
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }

                try {
                    byte[] bytes = loadClassBytes(name);
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Failed to read bytes for " + name, e);
                }
            }

            // For everything else, delegate to parent
            return super.loadClass(name);
        }

        private byte[] loadClassBytes(String className) throws IOException {
            String resourceName = "/" + className.replace('.', '/') + ".class";
            try (InputStream is = getClass().getResourceAsStream(resourceName)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + resourceName);
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    stream.write(buffer, 0, bytesRead);
                }
                return stream.toByteArray();
            }
        }
    }
}
