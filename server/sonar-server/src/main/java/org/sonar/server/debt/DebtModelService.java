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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.utils.ValidationMessages;

/**
 * Used through ruby code <pre>Internal.debt</pre>
 * Also used by SQALE plugin.
 */
public class DebtModelService implements DebtModel {

  private final DebtModelBackup debtModelBackup;

  public DebtModelService(DebtModelBackup debtModelBackup) {
    this.debtModelBackup = debtModelBackup;
  }

  @Override
  public List<DebtCharacteristic> characteristics() {
    return Collections.emptyList();
  }

  @Override
  public List<DebtCharacteristic> allCharacteristics() {
    return Collections.emptyList();
  }

  @Override
  @CheckForNull
  public DebtCharacteristic characteristicByKey(String key) {
    return null;
  }

  /**
   * Reset model
   */
  public void reset() {
    debtModelBackup.reset();
  }

  /**
   * Restore from XML
   */
  public ValidationMessages restoreFromXml(String xml) {
    return debtModelBackup.restoreFromXml(xml);
  }

  /**
   * Restore from XML and a given language
   */
  public ValidationMessages restoreFromXmlAndLanguage(String xml, String languageKey) {
    return debtModelBackup.restoreFromXml(xml, languageKey);
  }

  public String backup() {
    return debtModelBackup.backup();
  }

  public String backupFromLanguage(String languageKey) {
    return debtModelBackup.backup(languageKey);
  }

}
