/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { withAppState } from '../../../components/hoc/withAppState';
import { IndexationContextInterface, IndexationStatus } from '../../../types/indexation';
import { IndexationContext } from './IndexationContext';
import IndexationNotificationHelper from './IndexationNotificationHelper';

interface Props {
  appState: Pick<T.AppState, 'needIssueSync'>;
}

export class IndexationContextProvider extends React.PureComponent<
  React.PropsWithChildren<Props>,
  IndexationContextInterface
> {
  mounted = false;

  constructor(props: React.PropsWithChildren<Props>) {
    super(props);

    this.state = {
      status: { isCompleted: !props.appState.needIssueSync }
    };
  }

  componentDidMount() {
    this.mounted = true;

    if (!this.state.status.isCompleted) {
      IndexationNotificationHelper.startPolling(this.handleNewStatus);
    }
  }

  componentWillUnmount() {
    this.mounted = false;

    IndexationNotificationHelper.stopPolling();
  }

  handleNewStatus = (newIndexationStatus: IndexationStatus) => {
    if (newIndexationStatus.isCompleted) {
      IndexationNotificationHelper.stopPolling();
    }

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

export default withAppState(IndexationContextProvider);
