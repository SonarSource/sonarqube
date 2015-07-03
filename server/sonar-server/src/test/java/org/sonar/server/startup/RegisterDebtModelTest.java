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

package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.debt.DebtModelBackup;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterDebtModelTest {

  @Mock
  CharacteristicDao dao;

  @Mock
  DebtModelBackup debtModelBackup;

  RegisterDebtModel registerDebtModel;

  @Before
  public void setUp() {
    registerDebtModel = new RegisterDebtModel(dao, debtModelBackup);
  }

  @Test
  public void create_debt_model() {
    when(dao.selectEnabledCharacteristics()).thenReturn(Collections.<CharacteristicDto>emptyList());

    registerDebtModel.start();

    verify(debtModelBackup).reset();
  }

  @Test
  public void not_create_debt_model_if_already_exists() {
    when(dao.selectEnabledCharacteristics()).thenReturn(newArrayList(new CharacteristicDto()));

    registerDebtModel.start();

    verifyZeroInteractions(debtModelBackup);
  }
}
