/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.index;

import org.apache.commons.lang.RandomStringUtils;
import org.sonar.api.rule.Severity;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleTesting;

public class ActiveRuleDocTesting {

  public static ActiveRuleDoc newDoc() {
    return newDoc(ActiveRuleKey.of("sonar-way", RuleTesting.XOO_X1));
  }

  public static ActiveRuleDoc newDoc(ActiveRuleKey key) {
    return new ActiveRuleDoc(key)
      .setOrganizationUuid(RandomStringUtils.random(40))
      .setSeverity(Severity.CRITICAL)
      .setInheritance(null).setCreatedAt(150000000L)
      .setUpdatedAt(160000000L);
  }
}
