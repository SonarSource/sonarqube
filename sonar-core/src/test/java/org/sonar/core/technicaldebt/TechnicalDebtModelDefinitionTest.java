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
package org.sonar.core.technicaldebt;

import org.junit.Test;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TechnicalDebtModelDefinitionTest {

  @Test
  public void shouldCreateModel() throws Exception {
    TechnicalDebtManager technicalDebtManager = mock(TechnicalDebtManager.class);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    TechnicalDebtModelDefinition sqaleDefinition = new TechnicalDebtModelDefinition(technicalDebtManager, ruleFinder);

    sqaleDefinition.createModel();

    verify(technicalDebtManager, times(1)).createInitialModel(any(ValidationMessages.class), any(RuleCache.class));
  }
}
