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
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location } from '~sonar-aligned/types/router';
import { changePassword } from '../../api/users';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { AppState } from '../../types/appstate';
import ChangeAdminPasswordAppRenderer from './ChangeAdminPasswordAppRenderer';
import { DEFAULT_ADMIN_LOGIN } from './constants';

interface Props {
  appState: AppState;
  location: Location;
}

interface State {
  submitting: boolean;
  success: boolean;
}

export class ChangeAdminPasswordApp extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      submitting: false,
      success: !props.appState.instanceUsesDefaultAdminCredentials,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = async (password: string) => {
    this.setState({ submitting: true });
    let success = true;
    try {
      await changePassword({
        login: DEFAULT_ADMIN_LOGIN,
        password,
      });
    } catch (_) {
      success = false;
    }

    if (this.mounted) {
      this.setState({ submitting: false, success });
    }
  };

  render() {
    const {
      appState: { canAdmin },
      location,
    } = this.props;
    const { submitting, success } = this.state;
    return (
      <ChangeAdminPasswordAppRenderer
        canAdmin={canAdmin}
        onSubmit={this.handleSubmit}
        submitting={submitting}
        success={success}
        location={location}
      />
    );
  }
}

export default withRouter(withAppStateContext(ChangeAdminPasswordApp));
