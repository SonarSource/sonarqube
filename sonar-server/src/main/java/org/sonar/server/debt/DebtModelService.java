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

import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.utils.ValidationMessages;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Used through ruby code <pre>Internal.debt</pre>
 * Also used by SQALE plugin.
 */
public class DebtModelService implements DebtModel {

  private final DebtModelOperations debtModelOperations;
  private final DebtModelLookup debtModelLookup;
  private final DebtModelRestore debtModelRestore;

  public DebtModelService(DebtModelOperations debtModelOperations, DebtModelLookup debtModelLookup, DebtModelRestore debtModelRestore) {
    this.debtModelOperations = debtModelOperations;
    this.debtModelLookup = debtModelLookup;
    this.debtModelRestore = debtModelRestore;
  }

  public List<DebtCharacteristic> characteristics() {
    return debtModelLookup.rootCharacteristics();
  }

  public List<DebtCharacteristic> allCharacteristics() {
    return debtModelLookup.allCharacteristics();
  }

  @CheckForNull
  public DebtCharacteristic characteristicById(int id) {
    return debtModelLookup.characteristicById(id);
  }

  public DebtCharacteristic create(String name, @Nullable Integer parentId) {
    return debtModelOperations.create(name, parentId);
  }

  public DebtCharacteristic rename(int characteristicId, String newName) {
    return debtModelOperations.rename(characteristicId, newName);
  }

  public DebtCharacteristic moveUp(int characteristicId) {
    return debtModelOperations.moveUp(characteristicId);
  }

  public DebtCharacteristic moveDown(int characteristicId) {
    return debtModelOperations.moveDown(characteristicId);
  }

  /**
   * Disable characteristic and sub characteristic or only sub characteristic.
   * Will also update every rules linked to sub characteristics by setting characteristic id to -1 and remove function, factor and offset.
   */
  public void delete(int characteristicId) {
    debtModelOperations.delete(characteristicId);
  }

  /**
   * Restore from provided model
   */
  public ValidationMessages restore(){
    return debtModelRestore.restore();
  }

  /**
   * Restore from plugins providing rules for a given language
   */
  public ValidationMessages restoreFromLanguage(String languageKey) {
    return debtModelRestore.restore(languageKey);
  }

  /**
   * Restore from XML
   */
  public ValidationMessages restoreFromXml(String xml){
    return debtModelRestore.restoreFromXml(xml);
  }

  /**
   * Restore from XML and a given language
   */
  public ValidationMessages restoreFromXmlAndLanguage(String xml, String languageKey) {
    return debtModelRestore.restoreFromXml(xml, languageKey);
  }

}
