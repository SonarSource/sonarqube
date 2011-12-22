/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.check;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.CheckMessages;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.java.squid.SquidScanner;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

public class CommentedOutCodeLineCheckTest {

  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    CommentedOutCodeLineCheck check = new CommentedOutCodeLineCheck();
    squid.registerVisitor(check);
    JavaAstScanner scanner = squid.register(JavaAstScanner.class);
    scanner.scanFile(SquidTestUtils.getInputFile("/rules/CommentedCode.java"));
    squid.decorateSourceCodeTreeWith(Metric.values());
    squid.register(SquidScanner.class).scan();
  }

  @Test
  public void testDetection() {
    SourceFile sourceFile = (SourceFile) squid.search("CommentedCode.java");
    CheckMessages checkMessages = new CheckMessages(sourceFile);

    checkMessages.assertNext().atLine(26).withMessage("It's better to remove commented-out line of code.");
    checkMessages.assertNext().atLine(27);
    checkMessages.assertNext().atLine(28);

    checkMessages.assertNext().atLine(32);

    checkMessages.assertNext().atLine(38);
    checkMessages.assertNext().atLine(39);
    checkMessages.assertNext().atLine(40);

    checkMessages.assertNext().atLine(44);

    checkMessages.assertNext().atLine(60);

    checkMessages.assertNext().atLine(69);

    checkMessages.assertNoMore();

    assertThat(sourceFile.getInt(Metric.COMMENT_LINES), is(40)); // TODO but in fact 46, so without fake-JSNI
    assertThat(sourceFile.getInt(Metric.COMMENT_BLANK_LINES), is(16));
  }

}
