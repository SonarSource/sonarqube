/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.duplications.statement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.sonar.duplications.token.Token;

public class StatementTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAcceptNull() {
    new Statement(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAcceptEmpty() {
    new Statement(new ArrayList<Token>());
  }

  @Test
  public void shouldCreateStatementFromListOfTokens() {
    Statement statement = new Statement(Arrays.asList(new Token("a", 1, 1), new Token("b", 2, 1)));
    assertThat(statement.getValue(), is("ab"));
    assertThat(statement.getStartLine(), is(1));
    assertThat(statement.getEndLine(), is(2));
  }

}
