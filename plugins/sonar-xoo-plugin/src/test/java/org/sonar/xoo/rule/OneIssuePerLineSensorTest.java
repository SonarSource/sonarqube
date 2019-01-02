/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;

public class OneIssuePerLineSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private OneIssuePerLineSensor sensor = new OneIssuePerLineSensor();

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
      assertThat(issue.gap()).isNull();
    }
  }

  @Test
  public void testForceSeverity() throws IOException {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n")
      .build();

    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.fileSystem().add(inputFile);
    context.settings().setProperty(OneIssuePerLineSensor.FORCE_SEVERITY_PROPERTY, "MINOR");

    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(10); // One issue per line
    for (Issue issue : context.allIssues()) {
      assertThat(issue.overriddenSeverity()).isEqualTo(Severity.MINOR);
    }
  }

  @Test
  public void testProvideGap() throws IOException {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n")
      .build();

    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.fileSystem().add(inputFile);
    context.settings().setProperty(OneIssuePerLineSensor.EFFORT_TO_FIX_PROPERTY, "1.2");

    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(10); // One issue per line
    for (Issue issue : context.allIssues()) {
      assertThat(issue.gap()).isEqualTo(1.2d);
    }
  }

  @Test
  public void testProvideGap_before_5_5() throws IOException {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n")
      .build();

    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.fileSystem().add(inputFile);
    context.settings().setProperty(OneIssuePerLineSensor.EFFORT_TO_FIX_PROPERTY, "1.2");
    context.setRuntime(SonarRuntimeImpl.forSonarQube(Version.parse("5.4"), SonarQubeSide.SCANNER));

    sensor.execute(context);

    assertThat(context.allIssues()).hasSize(10); // One issue per line
    for (Issue issue : context.allIssues()) {
      assertThat(issue.gap()).isEqualTo(1.2d);
    }
  }

}
