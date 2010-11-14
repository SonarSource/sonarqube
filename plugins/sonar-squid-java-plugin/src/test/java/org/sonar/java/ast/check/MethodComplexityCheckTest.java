package org.sonar.java.ast.check;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

public class MethodComplexityCheckTest {
  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    MethodComplexityCheck check = new MethodComplexityCheck();
    check.setThreshold(5);
    squid.registerVisitor(check);
    JavaAstScanner scanner = squid.register(JavaAstScanner.class);
    scanner.scanFile(getFile("/metrics/branches/ComplexBranches.java"));
  }

  @Test
  public void testMethodComplexityExceedsThreshold() {
    SourceFile file = (SourceFile) squid.search("ComplexBranches.java");
    assertThat(file.getCheckMessages().size(), is(1));
    CheckMessage message = file.getCheckMessages().iterator().next();
    assertThat(message.getLine(), is(10));
  }
}
