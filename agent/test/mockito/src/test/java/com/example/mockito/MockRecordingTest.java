package com.example.mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Recording a mock must not disturb it.
 *
 * <p>
 * Mockito keeps thread-local state between one call and the next: argument
 * matchers go on a stack and are bound to the next invocation it sees, and
 * {@code when()} applies to the last invocation. Calling a method on a mock is
 * an invocation, so if AppMap calls one while recording -- {@code toString()},
 * say, to get a value for the AppMap -- Mockito attributes that state to the
 * wrong call. The stubbing the test set up then silently never applies, and the
 * method returns the Java type default instead.
 */
public class MockRecordingTest {

  /** Baseline: recording must not change what a stubbed mock returns. */
  @Test
  public void testStubbedValueSurvivesRecording() {
    Calculator calc = mock(Calculator.class);
    when(calc.add(1, 2)).thenReturn(3);

    Service service = new Service(calc);
    assertEquals(3, service.compute(1, 2));
  }

  /** Recording must not register invocations of its own. */
  @Test
  public void testRecordingAMockAddsNoInteractions() {
    Calculator calc = mock(Calculator.class);
    Service service = new Service(calc);

    assertEquals("same", service.identify(calc));

    verifyNoMoreInteractions(calc);
  }

  /**
   * anyInt() pushes a matcher, then the instrumented tag() call hands AppMap a
   * mock to record, then eq() pushes the second matcher. If AppMap calls the
   * mock in between, Mockito binds a matcher to that call instead and reports
   * "2 matchers expected, 1 recorded".
   */
  @Test
  public void testRecordingDoesNotStealPendingMatcherWhenStubbing() {
    Calculator calc = mock(Calculator.class);
    Service service = new Service(calc);

    when(calc.add(anyInt(), eq(service.tag(calc)))).thenReturn(5);

    assertEquals(5, calc.add(1, 2));
  }

  /** The same window, on the verification side. */
  @Test
  public void testRecordingDoesNotStealPendingMatcherWhenVerifying() {
    Calculator calc = mock(Calculator.class);
    Service service = new Service(calc);

    calc.add(1, 2);

    verify(calc).add(anyInt(), eq(service.tag(calc)));
  }

  /**
   * The inline mock maker mocks a class by retransforming it, so a mock of a
   * bootstrap-loaded type like ArrayList *is* an ArrayList, loaded by the
   * bootstrap loader. Recognizing it means looking for Mockito somewhere other
   * than the mock's own class loader, which can't see it.
   *
   * <p>
   * On Java 8 the default mock maker is the subclass one, which generates an
   * ordinary subclass with a normal class loader, so this only exercises the
   * bootstrap path from Java 11 on.
   */
  @Test
  public void testRecordingDoesNotStealPendingMatcherForBootstrapTypeMock() {
    Calculator calc = mock(Calculator.class);
    @SuppressWarnings("unchecked")
    List<String> list = mock(ArrayList.class);
    Service service = new Service(calc);

    when(calc.add(anyInt(), eq(service.tagAny(list)))).thenReturn(5);

    assertEquals(5, calc.add(1, 2));
  }

  /** A mocked bootstrap-loaded type must not be called while recording. */
  @Test
  public void testMockOfBootstrapTypeAddsNoInteractions() {
    @SuppressWarnings("unchecked")
    List<String> list = mock(ArrayList.class);
    Service service = new Service(mock(Calculator.class));

    assertEquals(2, service.tagAny(list));

    verifyNoMoreInteractions(list);
  }
}
