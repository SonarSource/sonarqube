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
import { connect } from 'react-redux';
import GlobalNavBranding, { SonarCloudNavBranding } from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavExplore from './GlobalNavExplore';
import GlobalNavNotifications from './GlobalNavNotifications';
import GlobalNavUserContainer from './GlobalNavUserContainer';
import Search from '../../search/Search';
import EmbedDocsPopupHelper from '../../embed-docs-modal/EmbedDocsPopupHelper';
import * as theme from '../../../theme';
import NavBar from '../../../../components/nav/NavBar';
import { lazyLoad } from '../../../../components/lazyLoad';
import { getCurrentUser, getAppState, Store } from '../../../../store/rootReducer';
import { isSonarCloud } from '../../../../helpers/system';
import { isLoggedIn } from '../../../../helpers/users';
import { OnboardingContext } from '../../OnboardingContext';
import './GlobalNav.css';

const GlobalNavPlus = lazyLoad(() => import('./GlobalNavPlus'), 'GlobalNavPlus');

interface StateProps {
  appState: Pick<T.AppState, 'canAdmin' | 'globalPages' | 'organizationsEnabled' | 'qualifiers'>;
  currentUser: T.CurrentUser;
}

interface OwnProps {
  location: { pathname: string };
}

type Props = StateProps & OwnProps;

export class GlobalNav extends React.PureComponent<Props> {
  render() {
    const { appState, currentUser } = this.props;
    return (
      <NavBar className="navbar-global" height={theme.globalNavHeightRaw} id="global-navigation">
        {isSonarCloud() ? <SonarCloudNavBranding /> : <GlobalNavBranding />}

        <GlobalNavMenu {...this.props} />

        <ul className="global-navbar-menu global-navbar-menu-right">
          {isSonarCloud() && <GlobalNavNotifications />}
          {isSonarCloud() && <GlobalNavExplore location={this.props.location} />}
          <EmbedDocsPopupHelper />
          <Search appState={appState} currentUser={currentUser} />
          {isLoggedIn(currentUser) && (
            <OnboardingContext.Consumer data-test="global-nav-plus">
              {openProjectOnboarding => (
                <GlobalNavPlus
                  appState={appState}
                  currentUser={currentUser}
                  openProjectOnboarding={openProjectOnboarding}
                />
              )}
            </OnboardingContext.Consumer>
          )}
          <GlobalNavUserContainer appState={appState} currentUser={currentUser} />
        </ul>
      </NavBar>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  currentUser: getCurrentUser(state),
  appState: getAppState(state)
});

export default connect(mapStateToProps)(GlobalNav);
