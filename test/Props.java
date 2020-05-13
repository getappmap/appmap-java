public class Props {
  public static void main(String[] argv) {
    for (String prop : argv) {
      System.out.printf("%s=\"%s\"\n", prop.replace('.', '_').toUpperCase(), System.getProperty(prop));
    }
  }
}
