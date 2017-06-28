package org.sonar.api.batch.fs.internal.charhandler;

import java.nio.charset.Charset;

import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class LineCounter extends CharHandler {
  private static final Logger LOG = Loggers.get(LineCounter.class);
    
  private int lines = 1;
  private int nonBlankLines = 0;
  private boolean blankLine = true;
  boolean alreadyLoggedInvalidCharacter = false;
  private final String filePath;
  private final Charset encoding;

  public LineCounter(String filePath, Charset encoding) {
    this.filePath = filePath;
    this.encoding = encoding;
  }

  @Override
  public void handleAll(char c) {
    if (!alreadyLoggedInvalidCharacter && c == '\ufffd') {
      LOG.warn("Invalid character encountered in file {} at line {} for encoding {}. Please fix file content or configure the encoding to be used using property '{}'.", filePath,
        lines, encoding, CoreProperties.ENCODING_PROPERTY);
      alreadyLoggedInvalidCharacter = true;
    }
  }

  @Override
  public void newLine() {
    lines++;
    if (!blankLine) {
      nonBlankLines++;
    }
    blankLine = true;
  }

  @Override
  public void handleIgnoreEoL(char c) {
    if (!Character.isWhitespace(c)) {
      blankLine = false;
    }
  }

  @Override
  public void eof() {
    if (!blankLine) {
      nonBlankLines++;
    }
  }

  public int lines() {
    return lines;
  }

  public int nonBlankLines() {
    return nonBlankLines;
  }

}