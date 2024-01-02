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
package org.sonar.server.projecttag;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.server.es.ProjectIndexer.Cause.PROJECT_TAGS_UPDATE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class TagsWsSupport {
  /**
   * The characters allowed in project tags are lower-case
   * letters, digits, plus (+), sharp (#), dash (-) and dot (.)
   */
  private static final Pattern VALID_TAG_REGEXP = Pattern.compile("[a-z0-9+#\\-.]+$");

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final ProjectIndexers projectIndexers;
  private final System2 system2;

  public TagsWsSupport(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ProjectIndexers projectIndexers, System2 system2) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.projectIndexers = projectIndexers;
    this.system2 = system2;
  }

  public void updateProjectTags(DbSession dbSession, String projectKey, List<String> providedTags) {
    List<String> validatedTags = checkAndUnifyTags(providedTags);
    ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
    updateTagsForProjectsOrApplication(dbSession, validatedTags, project);
  }

  public void updateApplicationTags(DbSession dbSession, String applicationKey, List<String> providedTags) {
    List<String> validatedTags = checkAndUnifyTags(providedTags);
    ProjectDto application = componentFinder.getApplicationByKey(dbSession, applicationKey);
    updateTagsForProjectsOrApplication(dbSession, validatedTags, application);
  }

  private void updateTagsForProjectsOrApplication(DbSession dbSession, List<String> tags, ProjectDto projectOrApplication) {
    userSession.checkProjectPermission(UserRole.ADMIN, projectOrApplication);
    projectOrApplication.setTags(tags);
    projectOrApplication.setUpdatedAt(system2.now());
    dbClient.projectDao().updateTags(dbSession, projectOrApplication);

    projectIndexers.commitAndIndexProjects(dbSession, singletonList(projectOrApplication), PROJECT_TAGS_UPDATE);
  }

  public static List<String> checkAndUnifyTags(List<String> tags) {
    return tags.stream()
      .filter(StringUtils::isNotBlank)
      .map(t -> t.toLowerCase(Locale.ENGLISH))
      .map(TagsWsSupport::checkTag)
      .distinct()
      .collect(MoreCollectors.toList());
  }

  private static String checkTag(String tag) {
    checkRequest(VALID_TAG_REGEXP.matcher(tag).matches(), "Tag '%s' is invalid. Tags accept only the characters: a-z, 0-9, '+', '-', '#', '.'", tag);
    return tag;
  }
}
