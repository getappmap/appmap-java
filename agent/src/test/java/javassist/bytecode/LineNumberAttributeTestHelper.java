package javassist.bytecode;

/**
 * Test helper that creates a {@link LineNumberAttribute} by invoking its package-private
 * constructor. Used by tests that synthesize methods via Javassist and need a non-negative
 * line number on the resulting bytecode (e.g. so the agent does not treat the method as
 * runtime-generated).
 */
public final class LineNumberAttributeTestHelper {
  private LineNumberAttributeTestHelper() {}

  /**
   * Build a {@code LineNumberTable} with a single entry mapping pc=0 to the given line.
   */
  public static LineNumberAttribute singleEntry(ConstPool cp, int line) {
    byte[] info = new byte[6];
    // table_length (u2)
    info[0] = 0;
    info[1] = 1;
    // start_pc (u2) = 0
    info[2] = 0;
    info[3] = 0;
    // line_number (u2)
    info[4] = (byte)((line >> 8) & 0xff);
    info[5] = (byte)(line & 0xff);
    try {
      java.lang.reflect.Constructor<LineNumberAttribute> ctor =
          LineNumberAttribute.class.getDeclaredConstructor(ConstPool.class, byte[].class);
      ctor.setAccessible(true);
      return ctor.newInstance(cp, info);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
