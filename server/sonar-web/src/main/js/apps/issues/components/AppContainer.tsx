/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { connect } from 'react-redux';
import { searchIssues } from '../../../api/issues';
import { withRouter } from '../../../components/hoc/withRouter';
import { lazyLoadComponent } from '../../../components/lazyLoadComponent';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { fetchBranchStatus } from '../../../store/rootActions';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import { FetchIssuesPromise } from '../../../types/issues';
import { RawQuery } from '../../../types/types';

const IssuesAppContainer = lazyLoadComponent(() => import('./IssuesApp'), 'IssuesAppContainer');

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state),
  fetchIssues
});

const fetchIssues = (query: RawQuery) => {
  return searchIssues({
    ...query,
    additionalFields: '_all',
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
  }).then(response => {
    const parsedIssues = response.issues.map(issue =>
      parseIssueFromResponse(issue, response.components, response.users, response.rules)
    );
    return { ...response, issues: parsedIssues } as FetchIssuesPromise;
  });
};

const mapDispatchToProps = { fetchBranchStatus };

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(IssuesAppContainer));
