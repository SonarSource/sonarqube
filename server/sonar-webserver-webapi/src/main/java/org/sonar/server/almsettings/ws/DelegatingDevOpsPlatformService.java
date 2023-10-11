/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.almsettings.ws;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Priority;
import org.apache.commons.lang.NotImplementedException;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.component.ComponentCreationData;

@ServerSide
@Priority(1)
public class DelegatingDevOpsPlatformService implements DevOpsPlatformService {

  private final Set<DevOpsPlatformService> delegates;

  public DelegatingDevOpsPlatformService(Set<DevOpsPlatformService> delegates) {
    this.delegates = delegates;
  }

  @Override
  public ALM getDevOpsPlatform() {
    throw new NotImplementedException();
  }

  @Override
  public Optional<DevOpsProjectDescriptor> getDevOpsProjectDescriptor(Map<String, String> characteristics) {
    return delegates.stream()
      .flatMap(delegate -> delegate.getDevOpsProjectDescriptor(characteristics).stream())
      .findFirst();
  }

  @Override
  public Optional<AlmSettingDto> getValidAlmSettingDto(DbSession dbSession, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    return findDelegate(devOpsProjectDescriptor.alm())
      .flatMap(delegate -> delegate.getValidAlmSettingDto(dbSession, devOpsProjectDescriptor));
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, String projectKey, AlmSettingDto almSettingDto,
    DevOpsProjectDescriptor devOpsProjectDescriptor) {
    return findDelegate(almSettingDto.getAlm())
      .map(delegate -> delegate.createProjectAndBindToDevOpsPlatform(dbSession, projectKey, almSettingDto, devOpsProjectDescriptor))
      .orElseThrow(() -> new IllegalStateException("Impossible to bind project to ALM platform " + almSettingDto.getAlm()));
  }

  @Override
  public ComponentCreationData createProjectAndBindToDevOpsPlatform(DbSession dbSession, AlmSettingDto almSettingDto, AccessToken accessToken,
    DevOpsProjectDescriptor devOpsProjectDescriptor) {
    return findDelegate(almSettingDto.getAlm())
      .map(delegate -> delegate.createProjectAndBindToDevOpsPlatform(dbSession, almSettingDto, accessToken, devOpsProjectDescriptor))
      .orElseThrow(() -> new IllegalStateException("Impossible to bind project to ALM platform " + almSettingDto.getAlm()));
  }

  private Optional<DevOpsPlatformService> findDelegate(ALM alm) {
    return delegates.stream()
      .filter(delegate -> delegate.getDevOpsPlatform().equals(alm))
      .findFirst();
  }

}
