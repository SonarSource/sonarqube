/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { CurrentUser, isLoggedInUser } from '../../app/types';
import { isMySet } from '../../apps/issues/utils';
import { getCurrentUser } from '../../store/rootReducer';
import { translate } from '../../helpers/l10n';
import * as urls from '../../helpers/urls';

interface Props {
  afterProjects?: JSX.Element;
  currentUser: CurrentUser;
  organization?: string;
}

// remove `: any` when https://github.com/DefinitelyTyped/DefinitelyTyped/issues/20249 is resolved
function CommonNavigation({ afterProjects, currentUser, organization }: Props): any {
  const issuesQuery =
    isLoggedInUser(currentUser) && isMySet()
      ? { resolved: 'false', myIssues: 'true' }
      : { resolved: 'false' };

  return [
    <li key="projects">
      <Link to={urls.getProjectsUrl(organization)} activeClassName="active">
        {translate('projects.page')}
      </Link>
    </li>,
    afterProjects,
    <li key="issues">
      <Link to={urls.getIssuesUrl(issuesQuery, organization)} activeClassName="active">
        {translate('issues.page')}
      </Link>
    </li>,
    <li key="coding-rules">
      <Link to={urls.getRulesUrl({}, organization)} activeClassName="active">
        {translate('coding_rules.page')}
      </Link>
    </li>,
    <li key="quality-profiles">
      <Link to={urls.getProfilesUrl(organization)} activeClassName="active">
        {translate('quality_profiles.page')}
      </Link>
    </li>,
    <li key="quality-gates">
      <Link to={urls.getQualityGatesUrl(organization)} activeClassName="active">
        {translate('quality_gates.page')}
      </Link>
    </li>
  ];
}

const mapStateToProps = (state: any) => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(CommonNavigation);
