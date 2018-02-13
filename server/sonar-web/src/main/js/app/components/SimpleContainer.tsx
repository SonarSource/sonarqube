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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import GlobalLoading from './GlobalLoading';
import GlobalFooterContainer from './GlobalFooterContainer';
import * as theme from '../theme';
import { tryGetGlobalNavigation } from '../../api/nav';
import NavBar from '../../components/nav/NavBar';

interface Props {
  children?: React.ReactNode;
  hideLoggedInInfo?: boolean;
}

interface State {
  loading: boolean;
  onSonarCloud: boolean;
}

export default class SimpleContainer extends React.PureComponent<Props, State> {
  mounted = false;

  static childContextTypes = {
    onSonarCloud: PropTypes.bool
  };

  state: State = { loading: true, onSonarCloud: false };

  getChildContext() {
    return { onSonarCloud: this.state.onSonarCloud };
  }

  componentDidMount() {
    this.mounted = true;
    tryGetGlobalNavigation().then(
      appState => {
        if (this.mounted) {
          this.setState({
            loading: false,
            onSonarCloud: Boolean(
              appState.settings && appState.settings['sonar.sonarcloud.enabled'] === 'true'
            )
          });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  render() {
    if (this.state.loading) {
      return <GlobalLoading />;
    }
    return (
      <div className="global-container">
        <div className="page-wrapper" id="container">
          <NavBar className="navbar-global" height={theme.globalNavHeightRaw} />
          {this.props.children}
        </div>
        <GlobalFooterContainer hideLoggedInInfo={this.props.hideLoggedInInfo} />
      </div>
    );
  }
}
