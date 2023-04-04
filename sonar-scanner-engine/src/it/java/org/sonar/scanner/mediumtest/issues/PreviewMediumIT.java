/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.mediumtest.issues;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class PreviewMediumIT {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester();

  @Test
  public void failWhenUsingPreviewMode() {
    try {
      tester.newAnalysis()
        .properties(ImmutableMap.<String, String>builder()
          .put("sonar.analysis.mode", "preview").build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("The preview mode, along with the 'sonar.analysis.mode' parameter, is no more supported. You should stop using this parameter.");
    }
  }

  @Test
  public void failWhenUsingIssuesMode() {
    try {
      tester.newAnalysis()
        .properties(ImmutableMap.<String, String>builder()
          .put("sonar.analysis.mode", "issues").build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("The preview mode, along with the 'sonar.analysis.mode' parameter, is no more supported. You should stop using this parameter.");
    }
  }

}
