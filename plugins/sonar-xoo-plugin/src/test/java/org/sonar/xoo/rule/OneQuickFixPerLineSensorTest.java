/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.rule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.xoo.Xoo;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class OneQuickFixPerLineSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private OneQuickFixPerLineSensor sensor = new OneQuickFixPerLineSensor();

  @Test
  public void testDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.ruleRepositories()).containsOnly(XooRulesDefinition.XOO_REPOSITORY, XooRulesDefinition.XOO2_REPOSITORY);
  }

  @Test
  public void testRule() throws IOException {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n")
      .build();

    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.fileSystem().add(inputFile);
    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(10); // One issue per line
    for (Issue issue : context.allIssues()) {
      assertThat(issue.isQuickFixAvailable()).isTrue();
    }
  }

}
