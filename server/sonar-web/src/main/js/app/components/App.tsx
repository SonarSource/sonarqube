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
import * as React from 'react';
import { connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import GlobalLoading from './GlobalLoading';
import { fetchCurrentUser } from '../../store/users/actions';
import { fetchLanguages, fetchAppState } from '../../store/rootActions';

interface Props {
  children: JSX.Element;
  fetchAppState: () => Promise<any>;
  fetchCurrentUser: () => Promise<void>;
  fetchLanguages: () => Promise<void>;
}

interface State {
  branchesEnabled: boolean;
  loading: boolean;
  onSonarCloud: boolean;
}

class App extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { branchesEnabled: false, loading: true, onSonarCloud: false };

  static childContextTypes = {
    branchesEnabled: PropTypes.bool.isRequired,
    onSonarCloud: PropTypes.bool
  };

  getChildContext() {
    return { branchesEnabled: this.state.branchesEnabled, onSonarCloud: this.state.onSonarCloud };
  }

  componentDidMount() {
    this.mounted = true;

    this.props
      .fetchCurrentUser()
      .then(() => Promise.all([this.fetchAppState(), this.props.fetchLanguages()]))
      .then(this.finishLoading, () => {});
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchAppState = () => {
    return this.props.fetchAppState().then(appState => {
      if (this.mounted) {
        const onSonarCloud =
          appState.settings != undefined &&
          appState.settings['sonar.lf.sonarqube.com.enabled'] === 'true';
        this.setState({ branchesEnabled: appState.branchesEnabled, onSonarCloud });
      }
    });
  };

  finishLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    if (this.state.loading) {
      return <GlobalLoading />;
    }
    return this.props.children;
  }
}

export default connect(null, { fetchAppState, fetchCurrentUser, fetchLanguages })(App as any);
