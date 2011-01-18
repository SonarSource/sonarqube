package org.sonar.batch.bootstrapper;

import java.io.*;

final class BootstrapperIOUtils {

  private BootstrapperIOUtils() {
  }

  /**
   * The default buffer size to use.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Unconditionally close a <code>Closeable</code>.
   */
  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  /**
   * Get the contents of a <code>Reader</code> as a String.
   */
  public static String toString(Reader input) throws IOException {
    StringWriter sw = new StringWriter();
    copyLarge(input, sw);
    return sw.toString();
  }

  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   */
  public static long copyLarge(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   */
  public static long copyLarge(Reader input, Writer output) throws IOException {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Deletes a file (not a directory).
   */
  public static boolean deleteFileQuietly(File file) {
    if (file == null) {
      return false;
    }
    try {
      return file.delete();
    } catch (Exception e) {
      return false;
    }
  }

}
