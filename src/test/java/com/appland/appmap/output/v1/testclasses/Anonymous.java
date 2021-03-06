package com.appland.appmap.output.v1.testclasses;

public class Anonymous {

    public static Runnable getAnonymousImpl(){
        return new Runnable() {
            @Override
            public void run() {
                System.err.println("Hello Anonymous!");
            }
        };
    }
}
