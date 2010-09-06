package org.sonar.colorizer;

import org.sonar.channel.Channel;

public class SyntaxHighlighterTestingHarness {

  public static String highlight(String input, Channel<HtmlCodeBuilder> tokenHighlighter) {
    TokenizerDispatcher syntaxHighlighter = new TokenizerDispatcher(tokenHighlighter);
    return syntaxHighlighter.colorize(input);
  }
}
