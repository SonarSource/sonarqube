package org.sonar.colorizer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.sonar.colorizer.SyntaxHighlighterTestingHarness.highlight;

import org.junit.Test;

public class RegexpTokenizerTest {

  RegexpTokenizer tokenHighlighter;;

  @Test
  public void testHighlight() {
    tokenHighlighter = new RegexpTokenizer("<r>", "</r>", "[0-9]+");
    assertThat(highlight("123, word = 435;", tokenHighlighter), is("<r>123</r>, word = <r>435</r>;"));
  }
  
  @Test
  public void testClone() {
    RegexpTokenizer tokenizer = new RegexpTokenizer("<r>", "</r>", "[a-z]+");
    RegexpTokenizer cloneTokenizer = tokenizer.clone();
    assertThat(tokenizer, is(not(cloneTokenizer)));
    assertThat(highlight("public 1234", cloneTokenizer), is("<r>public</r> 1234"));
  }

}
