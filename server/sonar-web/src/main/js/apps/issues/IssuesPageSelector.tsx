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
import * as React from 'react';
import { connect } from 'react-redux';
import AppContainer from './components/AppContainer';
import { RawQuery } from '../../helpers/query';
import { getCurrentUser, Store } from '../../store/rootReducer';
import { isSonarCloud } from '../../helpers/system';
import { isLoggedIn } from '../../helpers/users';

interface StateProps {
  currentUser: T.CurrentUser;
}

interface Props extends StateProps {
  location: { pathname: string; query: RawQuery };
}

function IssuesPage({ currentUser, location }: Props) {
  const myIssues = (isLoggedIn(currentUser) && isSonarCloud()) || undefined;
  return <AppContainer location={location} myIssues={myIssues} />;
}

const stateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default connect(stateToProps)(IssuesPage);
