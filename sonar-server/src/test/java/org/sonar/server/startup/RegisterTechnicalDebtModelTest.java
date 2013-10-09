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
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.technicaldebt.TechnicalDebtManager;
import org.sonar.core.technicaldebt.TechnicalDebtRuleCache;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RegisterTechnicalDebtModelTest {

  @Test
  public void shouldCreateModel() throws Exception {
    TechnicalDebtManager technicalDebtManager = mock(TechnicalDebtManager.class);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    RegisterTechnicalDebtModel sqaleDefinition = new RegisterTechnicalDebtModel(technicalDebtManager, ruleFinder, null);

    sqaleDefinition.start();

    verify(technicalDebtManager, times(1)).initAndMergePlugins(any(ValidationMessages.class), any(TechnicalDebtRuleCache.class));
  }
}
