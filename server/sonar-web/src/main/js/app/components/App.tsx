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
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
import { AppState, CurrentUser } from '../types';
import { fetchLanguages } from '../../store/rootActions';
import { fetchMyOrganizations } from '../../apps/account/organizations/actions';
import { getInstance, isSonarCloud } from '../../helpers/system';
import { lazyLoad } from '../../components/lazyLoad';
import { getCurrentUser, getAppState, getGlobalSettingValue, Store } from '../../store/rootReducer';
import { isLoggedIn } from '../../helpers/users';

const PageTracker = lazyLoad(() => import('./PageTracker'));

interface StateProps {
  appState: AppState | undefined;
  currentUser: CurrentUser | undefined;
  enableGravatar: boolean;
  gravatarServerUrl: string;
}

interface DispatchProps {
  fetchLanguages: () => Promise<void>;
  fetchMyOrganizations: () => Promise<void>;
}

interface OwnProps {
  children: JSX.Element;
}

type Props = StateProps & DispatchProps & OwnProps;

class App extends React.PureComponent<Props> {
  mounted = false;

  static childContextTypes = {
    branchesEnabled: PropTypes.bool.isRequired,
    canAdmin: PropTypes.bool.isRequired,
    organizationsEnabled: PropTypes.bool
  };

  getChildContext() {
    const { appState } = this.props;
    return {
      branchesEnabled: (appState && appState.branchesEnabled) || false,
      canAdmin: (appState && appState.canAdmin) || false,
      organizationsEnabled: (appState && appState.organizationsEnabled) || false
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.props.fetchLanguages();
    const { appState, currentUser } = this.props;
    if (appState && isSonarCloud() && currentUser && isLoggedIn(currentUser)) {
      this.props.fetchMyOrganizations();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  renderPreconnectLink = () => {
    const parser = document.createElement('a');
    parser.href = this.props.gravatarServerUrl;
    if (parser.hostname !== window.location.hostname) {
      return <link href={parser.origin} rel="preconnect" />;
    } else {
      return null;
    }
  };

  render() {
    return (
      <>
        <Helmet defaultTitle={getInstance()}>
          {this.props.enableGravatar && this.renderPreconnectLink()}
        </Helmet>
        {isSonarCloud() && <PageTracker />}
        {this.props.children}
      </>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const enableGravatar = getGlobalSettingValue(state, 'sonar.lf.enableGravatar');
  const gravatarServerUrl = getGlobalSettingValue(state, 'sonar.lf.gravatarServerUrl');
  return {
    appState: getAppState(state),
    currentUser: getCurrentUser(state),
    enableGravatar: Boolean(enableGravatar && enableGravatar.value === 'true'),
    gravatarServerUrl: (gravatarServerUrl && gravatarServerUrl.value) || ''
  };
};

const mapDispatchToProps = ({
  fetchLanguages,
  fetchMyOrganizations
} as any) as DispatchProps;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
