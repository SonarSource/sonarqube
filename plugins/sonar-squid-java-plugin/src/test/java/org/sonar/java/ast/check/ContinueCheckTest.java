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

public class ContinueCheckTest {

  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.registerVisitor(ContinueCheck.class);
    squid.register(JavaAstScanner.class).scanFile(
        getFile("/commons-collections-3.2.1/src/org/apache/commons/collections/ExtendedProperties.java"));
  }

  @Test
  public void testAvoidUsageOfContinue() {
    SourceFile file = (SourceFile) squid.search("org/apache/commons/collections/ExtendedProperties.java");
    assertThat(file.getCheckMessages().size(), is(1));
    CheckMessage message = file.getCheckMessages().iterator().next();
    assertThat(message.getLine(), is(566));
  }

}
