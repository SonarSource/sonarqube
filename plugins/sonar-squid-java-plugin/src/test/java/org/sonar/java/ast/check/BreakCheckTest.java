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

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.java.CheckMessages;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;

public class BreakCheckTest {

  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.registerVisitor(BreakCheck.class);

    File basedir = SquidTestUtils.getFile("/commons-collections-3.2.1/src");
    Collection<File> files = FileUtils.listFiles(new File(basedir, "org/apache/commons/collections/map"), new String[]{"java"}, true);

    squid.register(JavaAstScanner.class).scanFiles(InputFileUtils.create(basedir, files));
  }

  @Test
  public void testAvoidUsageOfBreakOutsideSwitch() {
    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("org/apache/commons/collections/map/LRUMap.java"));
    checkMessages.assertNext().atLine(244);
    checkMessages.assertNoMore();
  }

  @Test
  public void testAlowUsageOfBreakInsideSwitch() {
    CheckMessages checkMessages = new CheckMessages((SourceFile) squid.search("org/apache/commons/collections/map/Flat3Map.java"));
    checkMessages.assertNoMore();
  }

}
