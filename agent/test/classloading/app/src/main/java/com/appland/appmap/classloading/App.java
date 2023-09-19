package com.appland.appmap.classloading;

import static com.appland.appmap.util.ClassUtil.safeClassForName;
public class App {

    public static void main(String[] args) {
        try {
            safeClassForName(ClassLoader.getSystemClassLoader(), "javax.servlet.Filter");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }
}
