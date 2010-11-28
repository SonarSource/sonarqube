/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import static org.sonar.java.ast.SquidTestUtils.getFile;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.java.squid.SquidScanner;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import com.google.common.collect.Lists;

public class UndocumentedApiCheckTest {

  private Squid squid;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.registerVisitor(UndocumentedApiCheck.class);
    squid.register(JavaAstScanner.class).scanFile(getFile("/rules/UndocumentedApi.java"));
    squid.decorateSourceCodeTreeWith(Metric.values());
    squid.register(SquidScanner.class).scan();
  }

  @Test
  public void testUndocumentedApi() {
    SourceFile file = (SourceFile) squid.search("UndocumentedApi.java");

    List<CheckMessage> messages = Lists.newArrayList(file.getCheckMessages());
    Collections.sort(messages, new Comparator<CheckMessage>() {
      public int compare(CheckMessage o1, CheckMessage o2) {
        return o1.getLine() - o2.getLine();
      }
    });

    assertThat(file.getInt(Metric.PUBLIC_API) - file.getInt(Metric.PUBLIC_DOC_API), is(3));
    assertThat(file.getCheckMessages().size(), is(3));

    assertThat(messages.get(0).getLine(), is(10));
    assertThat(messages.get(1).getLine(), is(14));
    assertThat(messages.get(2).getLine(), is(17));
  }
}
