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
import GlobalNavBranding from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavExplore from './GlobalNavExplore';
import GlobalNavUserContainer from './GlobalNavUserContainer';
import GlobalNavPlus from './GlobalNavPlus';
import Search from '../../search/Search';
import EmbedDocsPopupHelper from '../../embed-docs-modal/EmbedDocsPopupHelper';
import * as theme from '../../../theme';
import { isLoggedIn, CurrentUser, AppState } from '../../../types';
import OnboardingModal from '../../../../apps/tutorials/onboarding/OnboardingModal';
import NavBar from '../../../../components/nav/NavBar';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';
import { getCurrentUser, getAppState, getGlobalSettingValue } from '../../../../store/rootReducer';
import { skipOnboarding } from '../../../../store/users/actions';
import { SuggestionLink } from '../../embed-docs-modal/SuggestionsProvider';
import './GlobalNav.css';

interface StateProps {
  appState: AppState;
  currentUser: CurrentUser;
  onSonarCloud: boolean;
}

interface DispatchProps {
  skipOnboarding: () => void;
}

interface Props extends StateProps, DispatchProps {
  closeOnboardingTutorial: () => void;
  isOnboardingTutorialOpen: boolean;
  location: { pathname: string };
  openOnboardingTutorial: () => void;
  suggestions: Array<SuggestionLink>;
}

interface State {
  helpOpen: boolean;
  onboardingTutorialTooltip: boolean;
}

class GlobalNav extends React.PureComponent<Props, State> {
  interval?: number;
  state: State = { helpOpen: false, onboardingTutorialTooltip: false };

  componentDidMount() {
    if (this.props.currentUser.showOnboardingTutorial) {
      this.openOnboardingTutorial();
    }
  }

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  }

  openOnboardingTutorial = () => {
    this.setState({ helpOpen: false });
    this.props.openOnboardingTutorial();
  };

  closeOnboardingTutorial = () => {
    this.setState({ onboardingTutorialTooltip: true });
    this.props.skipOnboarding();
    this.props.closeOnboardingTutorial();
    this.interval = window.setInterval(() => {
      this.setState({ onboardingTutorialTooltip: false });
    }, 3000);
  };

  render() {
    return (
      <NavBar className="navbar-global" height={theme.globalNavHeightRaw} id="global-navigation">
        <GlobalNavBranding />

        <GlobalNavMenu {...this.props} />

        <ul className="global-navbar-menu pull-right">
          <GlobalNavExplore location={this.props.location} onSonarCloud={this.props.onSonarCloud} />
          <EmbedDocsPopupHelper
            currentUser={this.props.currentUser}
            showTooltip={this.state.onboardingTutorialTooltip}
            suggestions={this.props.suggestions}
            tooltip={!this.props.onSonarCloud}
          />
          <Search appState={this.props.appState} currentUser={this.props.currentUser} />
          {isLoggedIn(this.props.currentUser) &&
            this.props.onSonarCloud && (
              <Tooltip
                overlay={translate('tutorials.follow_later')}
                visible={this.state.onboardingTutorialTooltip}>
                <GlobalNavPlus openOnboardingTutorial={this.openOnboardingTutorial} />
              </Tooltip>
            )}
          <GlobalNavUserContainer {...this.props} />
        </ul>

        {this.props.isOnboardingTutorialOpen && (
          <OnboardingModal onFinish={this.closeOnboardingTutorial} />
        )}
      </NavBar>
    );
  }
}

const mapStateToProps = (state: any): StateProps => {
  const sonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');

  return {
    currentUser: getCurrentUser(state),
    appState: getAppState(state),
    onSonarCloud: Boolean(sonarCloudSetting && sonarCloudSetting.value === 'true')
  };
};

const mapDispatchToProps: DispatchProps = { skipOnboarding };

export default connect(mapStateToProps, mapDispatchToProps)(GlobalNav);
