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

import * as React from 'react';
import { getValue } from '../../../api/settings';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { AppState } from '../../../types/appstate';
import { SettingsKey } from '../../../types/settings';
import LifetimeInformationRenderer from './LifetimeInformationRenderer';

interface Props {
  appState: AppState;
}

interface State {
  branchAndPullRequestLifeTimeInDays?: string;
  loading: boolean;
}

class LifetimeInformation extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchBranchAndPullRequestLifetimeSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchBranchAndPullRequestLifetimeSetting() {
    getValue({ key: SettingsKey.DaysBeforeDeletingInactiveBranchesAndPRs }).then(
      (settings) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            branchAndPullRequestLifeTimeInDays: settings?.value,
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  }

  render() {
    const {
      appState: { canAdmin },
    } = this.props;
    const { branchAndPullRequestLifeTimeInDays, loading } = this.state;

    return (
      <LifetimeInformationRenderer
        branchAndPullRequestLifeTimeInDays={branchAndPullRequestLifeTimeInDays}
        canAdmin={canAdmin}
        loading={loading}
      />
    );
  }
}

export default withAppStateContext(LifetimeInformation);
