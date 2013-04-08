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
package org.sonar.core.issue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IssueDtoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void set_data_check_maximal_length() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Issue data must not exceed 1000 characters: ");

    StringBuilder s = new StringBuilder(4500);
    for (int i = 0; i < 4500; i++) {
      s.append('a');
    }
    new IssueDto().setData(s.toString());
  }

}
