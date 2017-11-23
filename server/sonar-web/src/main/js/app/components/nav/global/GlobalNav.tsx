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
import { Link } from 'react-router';
import GlobalNavBranding from './GlobalNavBranding';
import GlobalNavUserContainer from './GlobalNavUserContainer';
import Search from '../../search/Search';
import GlobalHelp from '../../help/GlobalHelp';
import NavBar from '../../../../components/nav/NavBar';
import Tooltip from '../../../../components/controls/Tooltip';
import HelpIcon from '../../../../components/icons-components/HelpIcon';
import OnboardingModal from '../../../../apps/tutorials/onboarding/OnboardingModal';
import { getCurrentUser, getAppState, getGlobalSettingValue } from '../../../../store/rootReducer';
import { AppState } from '../../../../store/appState/duck';
import { skipOnboarding } from '../../../../store/users/actions';
import { translate } from '../../../../helpers/l10n';
import { CurrentUser, isLoggedInUser } from '../../../types';
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
  location: { pathname: string };
}

interface State {
  helpOpen: boolean;
  onboardingTutorialOpen: boolean;
  onboardingTutorialTooltip: boolean;
}

class GlobalNav extends React.PureComponent<Props, State> {
  interval?: number;
  state: State = {
    helpOpen: false,
    onboardingTutorialOpen: false,
    onboardingTutorialTooltip: false
  };

  componentDidMount() {
    window.addEventListener('keypress', this.onKeyPress);
    if (this.props.currentUser.showOnboardingTutorial) {
      this.openOnboardingTutorial();
    }
  }

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
    window.removeEventListener('keypress', this.onKeyPress);
  }

  onKeyPress = (event: KeyboardEvent) => {
    const { tagName } = event.target as HTMLElement;
    const code = event.keyCode || event.which;
    const isInput = tagName === 'INPUT' || tagName === 'SELECT' || tagName === 'TEXTAREA';
    const isTriggerKey = code === 63;
    if (!isInput && isTriggerKey) {
      this.openHelp();
    }
  };

  handleHelpClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.openHelp();
  };

  openHelp = () => this.setState({ helpOpen: true });

  closeHelp = () => this.setState({ helpOpen: false });

  openOnboardingTutorial = () => this.setState({ helpOpen: false, onboardingTutorialOpen: true });

  closeOnboardingTutorial = () => {
    this.setState({ onboardingTutorialOpen: false, onboardingTutorialTooltip: true });
    this.props.skipOnboarding();
    this.interval = window.setInterval(() => {
      this.setState({ onboardingTutorialTooltip: false });
    }, 3000);
  };

  render() {
    return (
      <NavBar className="navbar-global" id="global-navigation" height={30}>
        <GlobalNavBranding />

        {isLoggedInUser(this.props.currentUser) && (
          <ul className="global-navbar-menu pull-left">
            <li>
              <Link to="/projects/favorite" activeClassName="active">
                {translate('my_projects')}
              </Link>
            </li>
          </ul>
        )}

        <ul className="global-navbar-menu pull-right">
          <Search appState={this.props.appState} currentUser={this.props.currentUser} />
          <li>
            <a className="navbar-help" onClick={this.handleHelpClick} href="#">
              {this.state.onboardingTutorialTooltip ? (
                <Tooltip
                  defaultVisible={true}
                  overlay={translate('tutorials.follow_later')}
                  trigger="manual">
                  <HelpIcon />
                </Tooltip>
              ) : (
                <HelpIcon />
              )}
            </a>
          </li>
          <GlobalNavUserContainer {...this.props} />
        </ul>

        {this.state.helpOpen && (
          <GlobalHelp
            currentUser={this.props.currentUser}
            onClose={this.closeHelp}
            onTutorialSelect={this.openOnboardingTutorial}
            onSonarCloud={this.props.onSonarCloud}
          />
        )}

        {this.state.onboardingTutorialOpen && (
          <OnboardingModal onFinish={this.closeOnboardingTutorial} />
        )}
      </NavBar>
    );
  }
}

const mapStateToProps = (state: any): StateProps => {
  const sonarCloudSetting = getGlobalSettingValue(state, 'sonar.sonarcloud.enabled');

  return {
    appState: getAppState(state),
    currentUser: getCurrentUser(state),
    onSonarCloud: sonarCloudSetting != null && sonarCloudSetting.value === 'true'
  };
};

const mapDispatchToProps = { skipOnboarding };

export default connect<StateProps, DispatchProps>(mapStateToProps, mapDispatchToProps)(GlobalNav);
