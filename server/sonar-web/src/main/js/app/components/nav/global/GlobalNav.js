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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import GlobalNavBranding from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavUserContainer from './GlobalNavUserContainer';
import Search from '../../search/Search';
import GlobalHelp from '../../help/GlobalHelp';
import HelpIcon from '../../../../components/icons-components/HelpIcon';
import OnboardingModal from '../../../../apps/tutorials/onboarding/OnboardingModal';
import { getCurrentUser, getAppState, getSettingValue } from '../../../../store/rootReducer';

type Props = {
  appState: { organizationsEnabled: boolean },
  currentUser: { isLoggedIn: boolean, showOnboardingTutorial: true },
  sonarCloud: boolean
};

type State = {
  helpOpen: boolean,
  onboardingTutorialOpen: boolean
};

class GlobalNav extends React.PureComponent {
  props: Props;
  state: State = { helpOpen: false, onboardingTutorialOpen: false };

  componentDidMount() {
    window.addEventListener('keypress', this.onKeyPress);
    if (this.props.currentUser.showOnboardingTutorial) {
      this.openOnboardingTutorial();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keypress', this.onKeyPress);
  }

  onKeyPress = e => {
    const tagName = e.target.tagName;
    const code = e.keyCode || e.which;
    const isInput = tagName === 'INPUT' || tagName === 'SELECT' || tagName === 'TEXTAREA';
    const isTriggerKey = code === 63;
    if (!isInput && isTriggerKey) {
      this.openHelp();
    }
  };

  handleHelpClick = event => {
    event.preventDefault();
    this.openHelp();
  };

  openHelp = () => this.setState({ helpOpen: true });

  closeHelp = () => this.setState({ helpOpen: false });

  openOnboardingTutorial = () => this.setState({ helpOpen: false, onboardingTutorialOpen: true });

  closeOnboardingTutorial = () => this.setState({ onboardingTutorialOpen: false });

  render() {
    return (
      <nav className="navbar navbar-global page-container" id="global-navigation">
        <div className="container">
          <GlobalNavBranding />

          <GlobalNavMenu {...this.props} />

          <ul className="nav navbar-nav navbar-right">
            <Search appState={this.props.appState} currentUser={this.props.currentUser} />
            <li>
              <a className="navbar-help" onClick={this.handleHelpClick} href="#">
                <HelpIcon />
              </a>
            </li>
            <GlobalNavUserContainer {...this.props} />
          </ul>
        </div>

        {this.state.helpOpen &&
          <GlobalHelp
            currentUser={this.props.currentUser}
            onClose={this.closeHelp}
            onTutorialSelect={this.openOnboardingTutorial}
            sonarCloud={this.props.sonarCloud}
          />}

        {this.state.onboardingTutorialOpen &&
          <OnboardingModal onClose={this.closeOnboardingTutorial} />}
      </nav>
    );
  }
}

const mapStateToProps = state => {
  const sonarCloudSetting = getSettingValue(state, 'sonar.lf.sonarqube.com.enabled');

  return {
    currentUser: getCurrentUser(state),
    appState: getAppState(state),
    sonarCloud: sonarCloudSetting != null && sonarCloudSetting.value === 'true'
  };
};

export default connect(mapStateToProps)(GlobalNav);
