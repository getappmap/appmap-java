package com.example.mockito;

/**
 * Instrumented app code that handles mocks. AppMap records the parameters and
 * return values of these methods, which is how it comes into contact with the
 * mocks a test passes around.
 */
public class Service {
  private final Calculator calculator;

  public Service(Calculator calculator) {
    this.calculator = calculator;
  }

  public int compute(int a, int b) {
    return calculator.add(a, b);
  }

  /**
   * Takes a mock and returns an int, so it can be called from inside the
   * argument list of a stubbed or verified call.
   */
  public int tag(Calculator other) {
    return other == calculator ? 2 : 0;
  }

  /** Like tag(), for a collaborator that isn't a Calculator. */
  public int tagAny(Object other) {
    return other == null ? 0 : 2;
  }

  /** Takes a mock, so AppMap records a mock as a parameter. */
  public String identify(Calculator other) {
    return other == calculator ? "same" : "different";
  }
}
