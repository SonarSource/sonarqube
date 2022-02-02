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
import * as React from 'react';
import { connect } from 'react-redux';
import AdminContext from '../../app/components/AdminContext';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { getGlobalSettingValue, Store } from '../../store/rootReducer';
import { EditionKey } from '../../types/editions';
import { AppState, RawQuery } from '../../types/types';
import App from './App';

interface OwnProps {
  location: { pathname: string; query: RawQuery };
  appState: AppState;
}

interface StateToProps {
  updateCenterActive: boolean;
}

const mapStateToProps = (state: Store) => {
  const updateCenterActive = getGlobalSettingValue(state, 'sonar.updatecenter.activate');
  return {
    updateCenterActive: Boolean(updateCenterActive && updateCenterActive.value === 'true')
  };
};

function WithAdminContext(props: StateToProps & OwnProps) {
  const propsToPass = {
    location: props.location,
    updateCenterActive: props.updateCenterActive,
    currentEdition: props.appState.edition as EditionKey,
    standaloneMode: props.appState.standalone
  };

  return (
    <AdminContext.Consumer>
      {({ fetchPendingPlugins, pendingPlugins }) => (
        <App
          fetchPendingPlugins={fetchPendingPlugins}
          pendingPlugins={pendingPlugins}
          {...propsToPass}
        />
      )}
    </AdminContext.Consumer>
  );
}

export default connect(mapStateToProps)(withAppStateContext(WithAdminContext));
