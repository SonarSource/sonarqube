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

package org.sonar.db.es;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class RuleExtensionId {
  private final String organizationUuid;
  private final String repositoryName;
  private final String ruleKey;
  private final String id;

  private static final Splitter ID_SPLITTER = Splitter.on(CharMatcher.anyOf(":|"));

  public RuleExtensionId(String organizationUuid, String repositoryName, String ruleKey) {
    this.organizationUuid = organizationUuid;
    this.repositoryName = repositoryName;
    this.ruleKey = ruleKey;
    this.id = format("%s:%s|%s",repositoryName,ruleKey,organizationUuid);
  }

  public RuleExtensionId(String ruleExtensionId) {
    List<String> splittedId = ID_SPLITTER.splitToList(ruleExtensionId);
    checkArgument(splittedId.size() == 3, "Incorrect Id %s", ruleExtensionId);
    this.id = ruleExtensionId;
    this.repositoryName = splittedId.get(0);
    this.ruleKey = splittedId.get(1);
    this.organizationUuid = splittedId.get(2);
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RuleExtensionId)) {
      return false;
    }

    RuleExtensionId that = (RuleExtensionId) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
