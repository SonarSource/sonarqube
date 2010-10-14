package org.sonar.channel;

import java.io.IOException;
import java.io.Reader;

/**
 * This class can be extended to provide filtering capabilities for the CodeReader class. <br>
 * The purpose is to filter the character flow before the CodeReader class passes it to the different channels. It is possible to give
 * several filters to a CodeReader: they will be called one after another, following the declaration order in the CodeReader constructor, to
 * sequentially filter the character flow.
 * 
 * @see CodeReader
 * @see CodeBufferTest#testCodeReaderFilter()
 * @see CodeBufferTest#testSeveralCodeReaderFilter()
 * 
 */
public abstract class CodeReaderFilter {

  /**
   * This method implements the filtering logic, that is:
   * <ul>
   * <li>
   * get the characters from the reader,</li>
   * <li>
   * filter the character flow (and grab more characters from the reader if the filtering removes some),</li>
   * <li>
   * and fill the given buffer to its full capacity with the filtered data.</li>
   * </ul>
   * 
   * @param reader
   *          the input character flow
   * @param filteredBuffer
   *          the output buffer that must contain the filtered data
   * @param offset
   *          the offset to start reading from the reader
   * @param lenght
   *          the number of characters to read from the reader
   * @return The number of characters read, or -1 if the end of the stream has been reached
   * @throws IOException
   *           If an I/O error occurs
   */
  public abstract int read(Reader reader, char[] filteredBuffer, int offset, int lenght) throws IOException;

}
