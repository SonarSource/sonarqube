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
  DebtModelOperations debtModelOperations;

  @Mock
  DebtModelLookup debtModelLookup;

  @Mock
  DebtModelBackup debtModelBackup;

  DebtModelService service;

  @Before
  public void setUp() {
    service = new DebtModelService(debtModelOperations, debtModelLookup, debtModelBackup);
  }

  @Test
  public void find_root_characteristics() {
    service.characteristics();
    verify(debtModelLookup).rootCharacteristics();
  }

  @Test
  public void find_all_characteristics() {
    service.allCharacteristics();
    verify(debtModelLookup).allCharacteristics();
  }

  @Test
  public void find_characteristic_by_id() {
    service.characteristicById(111);
    verify(debtModelLookup).characteristicById(111);
  }

  @Test
  public void find_characteristic_by_key() {
    service.characteristicByKey("MEMORY_EFFICIENCY");
    verify(debtModelLookup).characteristicByKey("MEMORY_EFFICIENCY");
  }

  @Test
  public void create_characteristic() {
    service.create("Compilation name", 1);
    verify(debtModelOperations).create("Compilation name", 1);
  }

  @Test
  public void rename_characteristic() {
    service.rename(10, "New Efficiency");
    verify(debtModelOperations).rename(10, "New Efficiency");
  }

  @Test
  public void move_up() {
    service.moveUp(10);
    verify(debtModelOperations).moveUp(10);
  }

  @Test
  public void move_down() {
    service.moveDown(10);
    verify(debtModelOperations).moveDown(10);
  }

  @Test
  public void delete_characteristic() {
    service.delete(2);
    verify(debtModelOperations).delete(2);
  }

  @Test
  public void reset_model() {
    service.reset();
    verify(debtModelBackup).reset();
  }

  @Test
  public void restore_xml() {
    service.restoreFromXml("<xml/>");
    verify(debtModelBackup).restoreFromXml("<xml/>");
  }

  @Test
  public void restore_from_xml_and_language() {
    service.restoreFromXmlAndLanguage("<xml/>", "xoo");
    verify(debtModelBackup).restoreFromXml("<xml/>", "xoo");
  }

  @Test
  public void backup() {
    service.backup();
    verify(debtModelBackup).backup();
  }

  @Test
  public void backup_fom_language() {
    service.backupFromLanguage("xoo");
    verify(debtModelBackup).backup("xoo");
  }
}
