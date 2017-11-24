/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import DefaultOrganizationNavigation from './DefaultOrganizationNavigation';
import NotFound from './NotFound';
import { Organization, Extension } from '../types';
import { getSettingsNavigation } from '../../api/nav';
import { getOrganization, getOrganizationNavigation } from '../../api/organizations';
import { AppState, setAdminPages } from '../../store/appState/duck';
import { getAppState } from '../../store/rootReducer';

interface StateProps {
  appState: AppState;
}

interface DispatchProps {
  setAdminPages: (extensions: Extension[]) => void;
}

interface Props extends StateProps, DispatchProps {
  children: JSX.Element;
  location: { pathname: string };
}

interface State {
  loading: boolean;
  organization?: Organization;
}

class DefaultOrganizationContainer extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchOrganization();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchOrganization = () => {
    const { defaultOrganization } = this.props.appState;

    if (defaultOrganization) {
      this.setState({ loading: true });
      Promise.all([
        getOrganization(defaultOrganization),
        getOrganizationNavigation(defaultOrganization),
        getSettingsNavigation()
      ]).then(
        ([organization, navigation, settings]) => {
          this.props.setAdminPages(settings.extensions);
          if (this.mounted) {
            this.setState({ loading: false, organization: { ...organization, ...navigation } });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  render() {
    if (this.state.loading) {
      return null;
    }

    if (!this.state.organization) {
      return <NotFound />;
    }

    return (
      <div>
        <DefaultOrganizationNavigation
          location={this.props.location}
          organization={this.state.organization}
        />
        {this.props.children}
      </div>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state)
});

const mapDispatchToProps = { setAdminPages };

export default connect<StateProps, DispatchProps>(mapStateToProps, mapDispatchToProps)(
  DefaultOrganizationContainer
);
