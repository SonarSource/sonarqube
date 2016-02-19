/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.debt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DebtModelServiceTest {

  @Mock
  DebtModelBackup debtModelBackup;

  DebtModelService underTest;

  @Before
  public void setUp() {
    underTest = new DebtModelService(debtModelBackup);
  }

  @Test
  public void reset_model() {
    underTest.reset();
    verify(debtModelBackup).reset();
  }

  @Test
  public void restore_xml() {
    underTest.restoreFromXml("<xml/>");
    verify(debtModelBackup).restoreFromXml("<xml/>");
  }

  @Test
  public void restore_from_xml_and_language() {
    underTest.restoreFromXmlAndLanguage("<xml/>", "xoo");
    verify(debtModelBackup).restoreFromXml("<xml/>", "xoo");
  }

  @Test
  public void backup() {
    underTest.backup();
    verify(debtModelBackup).backup();
  }

  @Test
  public void backup_fom_language() {
    underTest.backupFromLanguage("xoo");
    verify(debtModelBackup).backup("xoo");
  }
}
