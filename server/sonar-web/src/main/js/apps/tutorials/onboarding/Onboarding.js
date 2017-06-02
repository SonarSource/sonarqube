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
import TokenStep from './TokenStep';
import OrganizationStep from './OrganizationStep';
import { translate } from '../../../helpers/l10n';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import './styles.css';

type Props = {
  currentUser: { login: string, isLoggedIn: boolean },
  organizationsEnabled: boolean,
  sonarCloud: boolean
};

type State = {
  step: string
};

export default class Onboarding extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { step: props.organizationsEnabled ? 'organization' : 'token' };
  }

  componentDidMount() {
    if (!this.props.currentUser.isLoggedIn) {
      handleRequiredAuthentication();
    }
  }

  handleTokenDone = (/* token: string */) => {
    this.setState({ step: '' });
  };

  handleOrganizationDone = () => {
    this.setState({ step: 'token' });
  };

  render() {
    if (!this.props.currentUser.isLoggedIn) {
      return null;
    }

    const { organizationsEnabled, sonarCloud } = this.props;
    const { step } = this.state;

    return (
      <div className="page page-limited">
        <header className="page-header">
          <h1 className="page-title">
            {translate(sonarCloud ? 'onboarding.header.sonarcloud' : 'onboarding.header')}
          </h1>
          <div className="page-description">
            {translate('onboarding.header.description')}
          </div>
        </header>

        {organizationsEnabled &&
          <OrganizationStep
            currentUser={this.props.currentUser}
            onContinue={this.handleOrganizationDone}
            open={step === 'organization'}
            stepNumber={1}
          />}

        <TokenStep
          onContinue={this.handleTokenDone}
          open={step === 'token'}
          stepNumber={organizationsEnabled ? 2 : 1}
        />
      </div>
    );
  }
}
