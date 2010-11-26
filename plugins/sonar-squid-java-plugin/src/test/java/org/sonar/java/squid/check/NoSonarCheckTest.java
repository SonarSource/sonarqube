package org.sonar.java.squid.check;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.java.squid.SquidScanner;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

public class NoSonarCheckTest {

  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    NoSonarCheck check = new NoSonarCheck();
    squid.registerVisitor(check);
    JavaAstScanner scanner = squid.register(JavaAstScanner.class);
    scanner.scanFile(getFile("/rules/FileWithNOSONARTags.java"));
    scanner.scanFile(getFile("/rules/FileWithoutNOSONARTags.java"));
    squid.decorateSourceCodeTreeWith(Metric.values());
    squid.register(SquidScanner.class).scan();
  }

  @Test
  public void testNoSonarTagDetection() {
    SourceFile file = (SourceFile) squid.search("FileWithNOSONARTags.java");
    assertThat(file.getCheckMessages().size(), is(2));
  }

  @Test
  public void testNoSonarTagDetectionWhenNoTag() {
    SourceFile file = (SourceFile) squid.search("FileWithoutNOSONARTags.java");
    assertThat(file.getCheckMessages().size(), is(0));
  }

}
