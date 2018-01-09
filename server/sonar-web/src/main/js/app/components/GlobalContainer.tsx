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
import GlobalNav from './nav/global/GlobalNav';
import GlobalFooterContainer from './GlobalFooterContainer';
import GlobalMessagesContainer from './GlobalMessagesContainer';

interface Props {
  children: React.ReactNode;
  location: { pathname: string };
}

interface State {
  isOnboardingTutorialOpen: boolean;
}

export default class GlobalContainer extends React.PureComponent<Props, State> {
  static childContextTypes = {
    closeOnboardingTutorial: PropTypes.func,
    openOnboardingTutorial: PropTypes.func
  };

  constructor(props: Props) {
    super(props);
    this.state = { isOnboardingTutorialOpen: false };
  }

  getChildContext() {
    return {
      closeOnboardingTutorial: this.closeOnboardingTutorial,
      openOnboardingTutorial: this.openOnboardingTutorial
    };
  }

  openOnboardingTutorial = () => this.setState({ isOnboardingTutorialOpen: true });

  closeOnboardingTutorial = () => this.setState({ isOnboardingTutorialOpen: false });

  render() {
    // it is important to pass `location` down to `GlobalNav` to trigger render on url change

    return (
      <div className="global-container">
        <div className="page-wrapper" id="container">
          <div className="page-container">
            <GlobalNav
              closeOnboardingTutorial={this.closeOnboardingTutorial}
              isOnboardingTutorialOpen={this.state.isOnboardingTutorialOpen}
              location={this.props.location}
              openOnboardingTutorial={this.openOnboardingTutorial}
            />
            <GlobalMessagesContainer />
            {this.props.children}
          </div>
        </div>
        <GlobalFooterContainer />
      </div>
    );
  }
}
