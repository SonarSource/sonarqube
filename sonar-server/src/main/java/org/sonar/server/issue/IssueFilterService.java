/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class IssueFilterService implements ServerComponent {

  private IssueFilterDao issueFilterDao;
  private final IssueFinder issueFinder;

  public IssueFilterService(IssueFilterDao issueFilterDao, IssueFinder issueFinder) {
    this.issueFilterDao = issueFilterDao;
    this.issueFinder = issueFinder;
  }

  public DefaultIssueFilter create(DefaultIssueFilter issueFilter, IssueQuery issueQuery, UserSession userSession) {
    // TODO
//    checkAuthorization(userSession, project, UserRole.ADMIN);
    issueFilterDao.insert(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public DefaultIssueFilter update(DefaultIssueFilter issueFilter, UserSession userSession) {
    // TODO
//    checkAuthorization(userSession, project, UserRole.ADMIN);
    issueFilterDao.update(IssueFilterDto.toIssueFilter(issueFilter));
    return issueFilter;
  }

  public void delete(Long issueFilterId, UserSession userSession) {
    // TODO
    //checkAuthorization(userSession, findActionPlanDto(actionPlanKey).getProjectKey(), UserRole.ADMIN);
    issueFilterDao.delete(issueFilterId);
  }

  public IssueQueryResult execute(IssueQuery issueQuery) {
    return issueFinder.find(issueQuery);
  }

  public IssueQueryResult execute(Long issueFilterId) {
    IssueFilterDto issueFilterDto = issueFilterDao.selectById(issueFilterId);
    if (issueFilterDto == null) {
      // TODO throw 404
      throw new IllegalArgumentException("Issue filter " + issueFilterId + " has not been found.");
    }

    DefaultIssueFilter issueFilter = issueFilterDto.toIssueFilter();
    // convert data to issue query
    issueFilter.data();

//    return issueFinder.find(issueQuery);
    return null;
  }

  @VisibleForTesting
  Map<String, Object> dataAsMap(String data) {
    Map<String, Object> props = newHashMap();

    Iterable<String> keyValues = Splitter.on(DefaultIssueFilter.SEPARATOR).split(data);
    for (String keyValue : keyValues) {
      String[] keyValueSplit = StringUtils.split(keyValue, DefaultIssueFilter.KEY_VALUE_SEPARATOR);
      if (keyValueSplit.length != 2) {
        throw new IllegalArgumentException("Key value should be separate by a '"+ DefaultIssueFilter.KEY_VALUE_SEPARATOR + "'");
      }
      String key = keyValueSplit[0];
      String value = keyValueSplit[1];
      String[] listValues = StringUtils.split(value, DefaultIssueFilter.LIST_SEPARATOR);
      if (listValues.length > 1) {
        props.put(key, newArrayList(listValues));
      } else {
        props.put(key, value);
      }
    }
    return props;
  }

  @VisibleForTesting
  String mapAsdata(Map<String, Object> props) {
    StringBuilder stringBuilder = new StringBuilder();

    for (Map.Entry<String, Object> entries : props.entrySet()){
      String key = entries.getKey();
      Object value = entries.getValue();

      stringBuilder.append(key);
      stringBuilder.append(DefaultIssueFilter.KEY_VALUE_SEPARATOR);

      List valuesList = newArrayList();
      if (value instanceof List) {
        // assume that it contains only strings
        valuesList = (List) value;
      } else if (value instanceof CharSequence) {
        valuesList = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split((CharSequence) value));
      } else {
        stringBuilder.append(value);
      }
      for (Object valueList : valuesList) {
        stringBuilder.append(valueList);
        stringBuilder.append(DefaultIssueFilter.LIST_SEPARATOR);
      }

      stringBuilder.append(DefaultIssueFilter.SEPARATOR);
    }
    return stringBuilder.toString();
  }

}
