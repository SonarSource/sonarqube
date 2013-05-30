/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.checks;

import org.junit.Test;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;

import java.util.HashSet;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class NoSonarFilterTest {

  NoSonarFilter filter = new NoSonarFilter();

  @Test
  public void ignoreLinesCommentedWithNoSonar() {
    JavaFile javaFile = new JavaFile("org.foo.Bar");

    Set<Integer> noSonarLines = new HashSet<Integer>();
    noSonarLines.add(31);
    noSonarLines.add(55);
    filter.addResource(javaFile, noSonarLines);

    // violation on class
    assertThat(filter.isIgnored(new Violation(null, javaFile))).isFalse();

    // violation on lines
    assertThat(filter.isIgnored(new Violation(null, javaFile).setLineId(30))).isFalse();
    assertThat(filter.isIgnored(new Violation(null, javaFile).setLineId(31))).isTrue();
  }


  @Test
  public void doNotIgnoreWhenNotFoundInSquid() {
    JavaFile javaFile = new JavaFile("org.foo.Bar");
    assertThat(filter.isIgnored(new Violation(null, javaFile).setLineId(30))).isFalse();
  }

  @Test
  public void should_accept_violations_from_no_sonar_rules() throws Exception {
    // The "No Sonar" rule logs violations on the lines that are flagged with "NOSONAR" !!
    JavaFile javaFile = new JavaFile("org.foo.Bar");

    Set<Integer> noSonarLines = new HashSet<Integer>();
    noSonarLines.add(31);
    filter.addResource(javaFile, noSonarLines);

    Rule noSonarRule = new Rule("squid", "NoSonarCheck");
    assertThat(filter.isIgnored(new Violation(noSonarRule, javaFile).setLineId(31))).isFalse();

  }
}
