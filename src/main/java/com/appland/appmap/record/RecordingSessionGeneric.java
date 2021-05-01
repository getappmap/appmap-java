package com.appland.appmap.record;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;

import java.util.HashSet;
import java.util.Vector;

public class RecordingSessionGeneric implements IRecordingSession {
    protected Vector<Event> events = new Vector<Event>();
    protected HashSet<String> classReferences = new HashSet<>();

    public synchronized void add(Event event) {
        this.events.add(event);

        if (event.parentId == null) {
            this.classReferences.add(
                    event.definedClass +
                            ":" + event.methodId +
                            ":" + event.isStatic +
                            ":" + event.lineNumber);
        }
    }

    public void start() {
        throw new UnsupportedOperationException();
    }

    public String stop() {
        throw new UnsupportedOperationException();
    }

    protected CodeObjectTree getClassMap() {
        CodeObjectTree registeredObjects = Recorder.getInstance().getRegisteredObjects();
        CodeObjectTree classMap = new CodeObjectTree();
        for (String key : this.classReferences) {
            String[] parts = key.split(":");

            CodeObject methodBranch = registeredObjects.getMethodBranch(parts[0], parts[1], Boolean.valueOf(parts[2]), Integer.valueOf(parts[3]));
            if (methodBranch != null)
                classMap.add(methodBranch);
        }

        return classMap;
    }
}
