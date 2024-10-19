/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.common.almsettings;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Priority;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;

@ServerSide
@Priority(1)
public class DelegatingDevOpsProjectCreatorFactory implements DevOpsProjectCreatorFactory {

  private final Set<DevOpsProjectCreatorFactory> delegates;

  public DelegatingDevOpsProjectCreatorFactory(Set<DevOpsProjectCreatorFactory> delegates) {
    this.delegates = delegates;
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, Map<String, String> characteristics) {
    return delegates.stream()
      .flatMap(delegate -> delegate.getDevOpsProjectCreator(dbSession, characteristics).stream())
      .findFirst();
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    return delegates.stream()
      .flatMap(delegate -> delegate.getDevOpsProjectCreator(almSettingDto, devOpsProjectDescriptor).stream())
      .findFirst();
  }

}
