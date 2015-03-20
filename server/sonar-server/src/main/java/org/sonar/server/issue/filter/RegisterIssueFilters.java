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
package org.sonar.server.issue.filter;

import org.picocontainer.Startable;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;

import java.util.Date;

public class RegisterIssueFilters implements Startable {

  static final String BUILTIN_ISSUE_FILTER_UNRESOLVED = "Unresolved Issues";
  static final String BUILTIN_ISSUE_FILTER_HIDDEN_DEBT = "False Positive and Won't Fix Issues";
  static final String BUILTIN_ISSUE_FILTER_MY_UNRESOLVED = "My Unresolved Issues";

  private final IssueFilterDao issueFilterDao;

  private final LoadedTemplateDao loadedTemplateDao;

  private final System2 system;

  public RegisterIssueFilters(IssueFilterDao issueFilterDao, LoadedTemplateDao loadedTemplateDao, System2 system) {
    this.issueFilterDao = issueFilterDao;
    this.loadedTemplateDao = loadedTemplateDao;
    this.system = system;
  }

  @Override
  public void start() {
    if (shouldRegisterBuiltinIssueFilters()) {
      createBuiltinIssueFilters();
      registerBuiltinIssueFilters();
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  private boolean shouldRegisterBuiltinIssueFilters() {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.ISSUE_FILTER_TYPE, BUILTIN_ISSUE_FILTER_UNRESOLVED) == 0;
  }

  private void createBuiltinIssueFilters() {
    Date now = new Date(system.now());

    IssueFilterDto unresolvedIssueFilter = new IssueFilterDto().setName(BUILTIN_ISSUE_FILTER_UNRESOLVED)
      .setShared(true)
      .setCreatedAt(now)
      .setUpdatedAt(now)
      .setData("resolved=false");
    issueFilterDao.insert(unresolvedIssueFilter);

    IssueFilterDto hiddenDebtFilter = new IssueFilterDto().setName(BUILTIN_ISSUE_FILTER_HIDDEN_DEBT)
      .setShared(true)
      .setCreatedAt(now)
      .setUpdatedAt(now)
      .setData("resolutions=FALSE-POSITIVE,WONTFIX");
    issueFilterDao.insert(hiddenDebtFilter);

    IssueFilterDto myUnresolvedFilter = new IssueFilterDto().setName(BUILTIN_ISSUE_FILTER_MY_UNRESOLVED)
      .setShared(true)
      .setCreatedAt(now)
      .setUpdatedAt(now)
      .setData("resolved=false|assignees=__me__");
    issueFilterDao.insert(myUnresolvedFilter);

  }

  private void registerBuiltinIssueFilters() {
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_ISSUE_FILTER_UNRESOLVED, LoadedTemplateDto.ISSUE_FILTER_TYPE));
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_ISSUE_FILTER_HIDDEN_DEBT, LoadedTemplateDto.ISSUE_FILTER_TYPE));
    loadedTemplateDao.insert(new LoadedTemplateDto(BUILTIN_ISSUE_FILTER_MY_UNRESOLVED, LoadedTemplateDto.ISSUE_FILTER_TYPE));
  }
}
