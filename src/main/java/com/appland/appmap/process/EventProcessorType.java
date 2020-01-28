package com.appland.appmap.process;

public enum EventProcessorType {
  Null,
  PassThrough,
  HttpServlet,
  SqlJdbc,
  ServletFilter,
  ToggleRecord,
  HttpRequest,
}
