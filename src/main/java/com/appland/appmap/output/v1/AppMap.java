package com.appland.appmap.output.v1;

public class AppMap {
  public Integer version = 1;
  public Metadata metadata;
  public CodeObject[] classMap = new CodeObject[0];
  public Event[] events = new Event[0];
}