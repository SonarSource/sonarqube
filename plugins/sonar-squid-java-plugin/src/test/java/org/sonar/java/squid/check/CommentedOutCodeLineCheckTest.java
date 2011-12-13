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
package org.sonar.java.squid.check;

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
    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("CommentedCode.java"));
    checkMessages.assertNext().atLine(4);
    checkMessages.assertNext().atLine(17);
    checkMessages.assertNext().atLine(18);
    checkMessages.assertNext().atLine(19);
    checkMessages.assertNoMore();
  }

}
