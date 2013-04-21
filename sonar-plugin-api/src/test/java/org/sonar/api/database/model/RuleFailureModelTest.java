/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.database.model;

import org.apache.commons.lang.StringUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class RuleFailureModelTest {

  @Test
  public void trimAndAbbreviateMessage() {
    final RuleFailureModel violation = new RuleFailureModel();
    violation.setMessage("    " + StringUtils.repeat("a", RuleFailureModel.MESSAGE_COLUMN_SIZE * 2));
    assertThat(violation.getMessage().length(), is(RuleFailureModel.MESSAGE_COLUMN_SIZE));
    assertThat(violation.getMessage(), startsWith("aaaaa"));
  }

  /**
   * this is a strange behavior with default Oracle settings...
   * <p/>
   * See SONAR-1073 :
   * Oracle uses as default the setting NLS_LENGTH_SEMANTICS=BYTE. In this case the character columns are created as
   * VARCHAR2(500) instead of VARCHAR2(500 CHAR). So the columns are created with a limitation of 500 single byte characters.
   * In UTF-8 some special characters need up to 6 single byte characters.
   * The problem is that Hibernate checks that the message does not exceed 500 Unicode characters.
   */
  @Test
  public void abbreviateMessageFromSizeInCharacters() throws UnsupportedEncodingException {
    assertThat("\u20AC".length(), is(1));
    // but EURO symbol is encoded on three bytes
    assertThat("\u20AC".getBytes("UTF-8").length, is(3));

    final RuleFailureModel violation = new RuleFailureModel();
    violation.setMessage(StringUtils.repeat("€", RuleFailureModel.MESSAGE_COLUMN_SIZE));

    assertThat(violation.getMessage().length(), is(RuleFailureModel.MESSAGE_COLUMN_SIZE));

    // THIS IS THE BUG ON ORACLE !!!!!!!!
    assertThat(violation.getMessage().getBytes("UTF-8").length, greaterThan(RuleFailureModel.MESSAGE_COLUMN_SIZE));
  }
}
