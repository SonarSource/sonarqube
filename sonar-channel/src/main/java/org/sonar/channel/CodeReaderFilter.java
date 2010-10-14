package org.sonar.channel;

import java.io.IOException;
import java.io.Reader;

public abstract class CodeReaderFilter {

  public abstract int read(Reader in, char[] cbuf, int off, int len) throws IOException;

}
