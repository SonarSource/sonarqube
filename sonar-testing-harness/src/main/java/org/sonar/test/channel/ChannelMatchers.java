package org.sonar.test.channel;

import org.sonar.channel.CodeReader;

public class ChannelMatchers {

  public static <OUTPUT> ChannelMatcher<OUTPUT> consume(String sourceCode, OUTPUT output) {
    return new ChannelMatcher<OUTPUT>(sourceCode, output);
  }

  public static <OUTPUT> ChannelMatcher<OUTPUT> consume(CodeReader codeReader, OUTPUT output) {
    return new ChannelMatcher<OUTPUT>(codeReader, output);
  }

  public static ReaderHasNextCharMatcher hasNextChar(char nextChar) {
    return new ReaderHasNextCharMatcher(nextChar);
  }
}
