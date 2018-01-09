/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Dispatch } from 'redux';
import { uniq } from 'lodash';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import {
  getCurrentUser,
  areThereCustomOrganizations,
  getGlobalSettingValue
} from '../../../store/rootReducer';
import { getOrganizations } from '../../../api/organizations';
import { receiveOrganizations } from '../../../store/organizations/duck';
import { searchIssues } from '../../../api/issues';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { RawQuery } from '../../../helpers/query';
import { CurrentUser } from '../../../app/types';
import { lazyLoad } from '../../../components/lazyLoad';

interface StateProps {
  currentUser: CurrentUser;
  onSonarCloud: boolean;
}

const mapStateToProps = (state: any): StateProps => {
  const onSonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');
  return {
    currentUser: getCurrentUser(state),
    onSonarCloud: Boolean(onSonarCloudSetting && onSonarCloudSetting.value === 'true')
  };
};

const fetchIssueOrganizations = (issues: any[]) => (dispatch: Dispatch<any>) => {
  if (!issues.length) {
    return Promise.resolve();
  }

  const organizationKeys = uniq(issues.map(issue => issue.organization));
  return getOrganizations({ organizations: organizationKeys.join() }).then(
    response => dispatch(receiveOrganizations(response.organizations)),
    throwGlobalError
  );
};

const fetchIssues = (query: RawQuery, requestOrganizations = true) => (
  // use `Function` to be able to do `dispatch(...).then(...)`
  dispatch: Function,
  getState: () => any
) => {
  const organizationsEnabled = areThereCustomOrganizations(getState());
  return searchIssues({ ...query, additionalFields: '_all' })
    .then(response => {
      const parsedIssues = response.issues.map(issue =>
        parseIssueFromResponse(issue, response.components, response.users, response.rules)
      );
      return { ...response, issues: parsedIssues };
    })
    .then(response => {
      return organizationsEnabled && requestOrganizations
        ? dispatch(fetchIssueOrganizations(response.issues)).then(() => response)
        : response;
    })
    .catch(throwGlobalError);
};

interface DispatchProps {
  fetchIssues: (query: RawQuery, requestOrganizations?: boolean) => Promise<void>;
}

// have to type cast this, because of async action
const mapDispatchToProps = { fetchIssues: fetchIssues as any } as DispatchProps;

interface OwnProps {
  location: { pathname: string; query: RawQuery };
  myIssues?: boolean;
}

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  lazyLoad(() => import('./App'))
);
