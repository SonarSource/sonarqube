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
package org.sonar.server.common.almsettings.bitbucketcloud;

import java.util.Map;
import java.util.Optional;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.user.UserSession;

public class BitbucketCloudProjectCreatorFactory implements DevOpsProjectCreatorFactory {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final ProjectCreator projectCreator;
  private final ProjectKeyGenerator projectKeyGenerator;

  public BitbucketCloudProjectCreatorFactory(DbClient dbClient, UserSession userSession, BitbucketCloudRestClient bitbucketCloudRestClient, ProjectCreator projectCreator,
    ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.projectCreator = projectCreator;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(DbSession dbSession, Map<String, String> characteristics) {
    return Optional.empty();
  }

  @Override
  public Optional<DevOpsProjectCreator> getDevOpsProjectCreator(AlmSettingDto almSettingDto, DevOpsProjectDescriptor devOpsProjectDescriptor) {
    if (almSettingDto.getAlm() != ALM.BITBUCKET_CLOUD) {
      return Optional.empty();
    }
    return Optional.of(
      new BitbucketCloudProjectCreator(
        dbClient,
        almSettingDto,
        devOpsProjectDescriptor,
        userSession,
        bitbucketCloudRestClient,
        projectCreator,
        projectKeyGenerator));
  }
}
