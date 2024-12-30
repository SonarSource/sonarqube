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

/* eslint-disable react/no-unused-state */

import * as React from 'react';
import { AppState } from '../../../types/appstate';
import { IndexationContextInterface, IndexationStatus } from '../../../types/indexation';
import withAppStateContext from '../app-state/withAppStateContext';
import { IndexationContext } from './IndexationContext';
import IndexationNotificationHelper from './IndexationNotificationHelper';

export interface IndexationContextProviderProps {
  appState: AppState;
}

export class IndexationContextProvider extends React.PureComponent<
  React.PropsWithChildren<IndexationContextProviderProps>,
  IndexationContextInterface
> {
  mounted = false;

  componentDidMount() {
    this.mounted = true;

    if (this.props.appState.needIssueSync) {
      IndexationNotificationHelper.startPolling(this.handleNewStatus);
    } else {
      this.setState({
        status: { isCompleted: true, hasFailures: false },
      });
    }
  }

  componentWillUnmount() {
    this.mounted = false;

    IndexationNotificationHelper.stopPolling();
  }

  handleNewStatus = (newIndexationStatus: IndexationStatus) => {
    if (this.mounted) {
      this.setState({ status: newIndexationStatus });
    }
  };

  render() {
    return (
      <IndexationContext.Provider value={this.state}>
        {this.props.children}
      </IndexationContext.Provider>
    );
  }
}

export default withAppStateContext(IndexationContextProvider);
