package org.sonar.java.bytecode.check;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

public class DITCheckTest {

  private static Squid squid;

  @BeforeClass
  public static void setup() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(getFile("/bytecode/unusedProtectedMethod/src"));
    DITCheck check = new DITCheck();
    check.setThreshold(1);
    squid.registerVisitor(check);
    squid.register(BytecodeScanner.class).scanDirectory(getFile("/bytecode/unusedProtectedMethod/bin"));
  }

  @Test
  public void testDepthOfInheritanceExceedsThreshold() {
    SourceFile file = (SourceFile) squid.search("UnusedProtectedMethod.java");
    assertThat(file.getCheckMessages().size(), is(1));
    CheckMessage message = file.getCheckMessages().iterator().next();
    assertThat(message.getLine(), is(7));
  }

  @Test
  public void testDepthOfInheritanceNotExceedsThreshold() {
    SourceFile file = (SourceFile) squid.search("Job.java");
    assertThat(file.getCheckMessages().size(), is(0));
  }

}
