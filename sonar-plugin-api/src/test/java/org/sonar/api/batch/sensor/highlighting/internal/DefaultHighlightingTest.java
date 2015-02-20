package org.sonar.api.batch.sensor.highlighting.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.COMMENT;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.CPP_DOC;
import static org.sonar.api.batch.sensor.highlighting.TypeOfText.KEYWORD;

public class DefaultHighlightingTest {

  private Collection<SyntaxHighlightingRule> highlightingRules;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleRules() {

    DefaultHighlighting highlightingDataBuilder = new DefaultHighlighting()
      .onFile(new DefaultInputFile("foo", "src/Foo.java"))
      .highlight(0, 10, COMMENT)
      .highlight(10, 12, KEYWORD)
      .highlight(24, 38, KEYWORD)
      .highlight(42, 50, KEYWORD)
      .highlight(24, 65, CPP_DOC)
      .highlight(12, 20, COMMENT);

    highlightingRules = highlightingDataBuilder.getSyntaxHighlightingRuleSet();
  }

  @Test
  public void should_register_highlighting_rule() throws Exception {
    assertThat(highlightingRules).hasSize(6);
  }

  @Test
  public void should_order_by_start_then_end_offset() throws Exception {
    assertThat(highlightingRules).extracting("startPosition").containsOnly(0, 10, 12, 24, 24, 42);
    assertThat(highlightingRules).extracting("endPosition").containsOnly(10, 12, 20, 38, 65, 50);
    assertThat(highlightingRules).extracting("textType").containsOnly(COMMENT, KEYWORD, COMMENT, KEYWORD, CPP_DOC, KEYWORD);
  }

  @Test
  public void should_suport_overlapping() throws Exception {
    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(new DefaultInputFile("foo", "src/Foo.java"))
      .highlight(0, 15, KEYWORD)
      .highlight(8, 12, CPP_DOC)
      .save();
  }

  @Test
  public void should_prevent_boudaries_overlapping() throws Exception {
    throwable.expect(IllegalStateException.class);
    throwable.expectMessage("Cannot register highlighting rule for characters from 8 to 15 as it overlaps at least one existing rule");

    new DefaultHighlighting(mock(SensorStorage.class))
      .onFile(new DefaultInputFile("foo", "src/Foo.java"))
      .highlight(0, 10, KEYWORD)
      .highlight(8, 15, KEYWORD)
      .save();
  }

}
