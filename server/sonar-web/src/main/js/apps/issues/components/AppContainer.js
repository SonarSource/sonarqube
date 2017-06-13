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
// @flow
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import type { Dispatch } from 'redux';
import { uniq } from 'lodash';
import App from './App';
import { onFail } from '../../../store/rootActions';
import { getComponent, getCurrentUser } from '../../../store/rootReducer';
import { getOrganizations } from '../../../api/organizations';
import { receiveOrganizations } from '../../../store/organizations/duck';
import { searchIssues } from '../../../api/issues';
import { parseIssueFromResponse } from '../../../helpers/issues';
import type { RawQuery } from '../../../helpers/query';

const mapStateToProps = (state, ownProps) => ({
  component: ownProps.location.query.id
    ? getComponent(state, ownProps.location.query.id)
    : undefined,
  currentUser: getCurrentUser(state)
});

const fetchIssueOrganizations = issues => dispatch => {
  if (!issues.length) {
    return Promise.resolve();
  }

  const organizationKeys = uniq(issues.map(issue => issue.organization));
  return getOrganizations(organizationKeys).then(
    response => dispatch(receiveOrganizations(response.organizations)),
    onFail(dispatch)
  );
};

const fetchIssues = (query: RawQuery) => dispatch =>
  searchIssues({ ...query, additionalFields: '_all' })
    .then(response => {
      const parsedIssues = response.issues.map(issue =>
        parseIssueFromResponse(issue, response.components, response.users, response.rules)
      );
      return { ...response, issues: parsedIssues };
    })
    .then(response => dispatch(fetchIssueOrganizations(response.issues)).then(() => response))
    .catch(onFail(dispatch));

const onRequestFail = (error: Error) => (dispatch: Dispatch<*>) => onFail(dispatch)(error);

const mapDispatchToProps = { fetchIssues, onRequestFail };

export default connect(mapStateToProps, mapDispatchToProps)(withRouter(App));
