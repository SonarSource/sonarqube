/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.issue.tracking;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueTrackingBlocksRecognizerTest {

  @Test
  public void test() {
    assertThat(compute(t("abcde"), t("abcde"), 4, 4)).isEqualTo(5);
    assertThat(compute(t("abcde"), t("abcd"), 4, 4)).isEqualTo(4);
    assertThat(compute(t("bcde"), t("abcde"), 4, 4)).isEqualTo(0);
    assertThat(compute(t("bcde"), t("abcde"), 3, 4)).isEqualTo(4);
  }

  private static int compute(FileHashes a, FileHashes b, int ai, int bi) {
    IssueTrackingBlocksRecognizer rec = new IssueTrackingBlocksRecognizer(a, b);
    return rec.computeLengthOfMaximalBlock(ai, bi);
  }

  private static FileHashes t(String text) {
    String[] array = new String[text.length()];
    for (int i = 0; i < text.length(); i++) {
      array[i] = "" + text.charAt(i);
    }
    return FileHashes.create(array);
  }

}
