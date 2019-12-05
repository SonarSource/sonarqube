/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.almsettings;

import java.util.function.Consumer;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;

import static java.util.Arrays.stream;
import static org.sonar.db.almsettings.AlmSettingsTesting.newAzureAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newAzureProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGitlabAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGitlabProjectAlmSettingDto;

public class AlmSettingsDbTester {

  private final DbTester db;

  public AlmSettingsDbTester(DbTester db) {
    this.db = db;
  }

  @SafeVarargs
  public final AlmSettingDto insertGitHubAlmSetting(Consumer<AlmSettingDto>... populators) {
    return insert(newGithubAlmSettingDto(), populators);
  }

  @SafeVarargs
  public final AlmSettingDto insertAzureAlmSetting(Consumer<AlmSettingDto>... populators) {
    return insert(newAzureAlmSettingDto(), populators);
  }

  @SafeVarargs
  public final AlmSettingDto insertGitlabAlmSetting(Consumer<AlmSettingDto>... populators) {
    return insert(newGitlabAlmSettingDto(), populators);
  }

  @SafeVarargs
  public final AlmSettingDto insertBitbucketAlmSetting(Consumer<AlmSettingDto>... populators) {
    return insert(newBitbucketAlmSettingDto(), populators);
  }

  @SafeVarargs
  public final ProjectAlmSettingDto insertGitHubProjectAlmSetting(AlmSettingDto githubAlmSetting, ComponentDto project, Consumer<ProjectAlmSettingDto>... populators) {
    return insertProjectAlmSetting(newGithubProjectAlmSettingDto(githubAlmSetting, project), populators);
  }

  public ProjectAlmSettingDto insertAzureProjectAlmSetting(AlmSettingDto azureAlmSetting, ComponentDto project) {
    return insertProjectAlmSetting(newAzureProjectAlmSettingDto(azureAlmSetting, project));
  }

  public ProjectAlmSettingDto insertGitlabProjectAlmSetting(AlmSettingDto gitlabAlmSetting, ComponentDto project) {
    return insertProjectAlmSetting(newGitlabProjectAlmSettingDto(gitlabAlmSetting, project));
  }

  @SafeVarargs
  public final ProjectAlmSettingDto insertBitbucketProjectAlmSetting(AlmSettingDto bitbucketAlmSetting, ComponentDto project, Consumer<ProjectAlmSettingDto>... populators) {
    return insertProjectAlmSetting(newBitbucketProjectAlmSettingDto(bitbucketAlmSetting, project), populators);
  }

  @SafeVarargs
  private final ProjectAlmSettingDto insertProjectAlmSetting(ProjectAlmSettingDto dto, Consumer<ProjectAlmSettingDto>... populators) {
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().projectAlmSettingDao().insertOrUpdate(db.getSession(), dto);
    db.commit();
    return dto;
  }

  private AlmSettingDto insert(AlmSettingDto dto, Consumer<AlmSettingDto>[] populators) {
    stream(populators).forEach(p -> p.accept(dto));
    db.getDbClient().almSettingDao().insert(db.getSession(), dto);
    db.commit();
    return dto;
  }

}
