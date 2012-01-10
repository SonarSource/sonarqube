/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.java.ast.visitor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.AnalysisException;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.*;

public class PackageVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test(expected = AnalysisException.class)
  public void shouldFailIfPackageDifferentThanPhysicalDirectory() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/wrongPackages/", "org/foo/WrongPackage.java"));
    squid.aggregate();
  }

  @Test(expected = AnalysisException.class)
  public void shouldFailIfBadSourceDirectory() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/wrongPackages/org", "foo/GoodPackage.java"));
    squid.aggregate();
  }

  @Test
  public void shouldGuessPackageWhenCommentedOutFile() {
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/special_cases/wrongPackages", "org/foo/CommentedOutFile.java"));
    SourceProject project = squid.aggregate();

    assertNull(squid.search("CommentedOutFile.java"));
    assertNotNull(squid.search("org/foo/CommentedOutFile.java"));
    assertNotNull(squid.search("org/foo"));
    assertEquals(1, project.getInt(Metric.PACKAGES));
  }
}
