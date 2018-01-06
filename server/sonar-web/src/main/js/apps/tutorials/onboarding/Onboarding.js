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
// @flow
import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import TokenStep from './TokenStep';
import OrganizationStep from './OrganizationStep';
import AnalysisStep from './AnalysisStep';
import ProjectWatcher from './ProjectWatcher';
import { skipOnboarding } from '../../../api/users';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import './styles.css';

/*::
type Props = {|
  currentUser: { login: string, isLoggedIn: boolean },
  onFinish: () => void,
  organizationsEnabled: boolean,
  sonarCloud: boolean
|};
*/

/*::
type State = {
  finished: boolean,
  organization?: string,
  projectKey?: string,
  skipping: boolean,
  step: string,
  token?: string
};
*/

export default class Onboarding extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      finished: false,
      skipping: false,
      step: props.organizationsEnabled ? 'organization' : 'token'
    };
  }

  componentDidMount() {
    this.mounted = true;

    // useCapture = true to receive the event before inputs
    window.addEventListener('keydown', this.onKeyDown, true);

    if (!this.props.currentUser.isLoggedIn) {
      handleRequiredAuthentication();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.onKeyDown, true);
    this.mounted = false;
  }

  onKeyDown = (event /*: KeyboardEvent */) => {
    // ESC key
    if (event.keyCode === 27) {
      this.finishOnboarding();
    }
  };

  finishOnboarding = () => {
    this.setState({ skipping: true });
    skipOnboarding().then(
      () => {
        if (this.mounted) {
          this.props.onFinish();

          if (this.state.projectKey) {
            this.context.router.push(getProjectUrl(this.state.projectKey));
          }
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ skipping: false });
        }
      }
    );
  };

  handleTimeout = () => {
    // unset `projectKey` to display a generic "Finish this tutorial" button
    this.setState({ projectKey: undefined });
  };

  handleTokenDone = (token /*: string */) => {
    this.setState({ step: 'analysis', token });
  };

  handleOrganizationDone = (organization /*: string */) => {
    this.setState({ organization, step: 'token' });
  };

  handleTokenOpen = () => this.setState({ step: 'token' });

  handleOrganizationOpen = () => this.setState({ step: 'organization' });

  handleSkipClick = (event /*: Event */) => {
    event.preventDefault();
    this.finishOnboarding();
  };

  handleFinish = (projectKey /*: string | void */) => this.setState({ finished: true, projectKey });

  handleReset = () => this.setState({ finished: false, projectKey: undefined });

  render() {
    if (!this.props.currentUser.isLoggedIn) {
      return null;
    }

    const { organizationsEnabled, sonarCloud } = this.props;
    const { step, token } = this.state;

    let stepNumber = 1;

    const header = translate(sonarCloud ? 'onboarding.header.sonarcloud' : 'onboarding.header');

    return (
      <div className="modal-container">
        <Helmet title={header} titleTemplate="%s" />

        <div className="page page-limited onboarding">
          <header className="page-header">
            <h1 className="page-title">{header}</h1>
            <div className="page-actions">
              {this.state.skipping ? (
                <i className="spinner" />
              ) : (
                <a className="js-skip text-muted" href="#" onClick={this.handleSkipClick}>
                  {translate('tutorials.skip')}
                </a>
              )}
              <p className="note">
                {translate(
                  sonarCloud ? 'tutorials.find_it_back_in_plus' : 'tutorials.find_it_back_in_help'
                )}
              </p>
            </div>
            <div className="page-description">
              {translateWithParameters(
                'onboarding.header.description',
                organizationsEnabled ? 3 : 2
              )}
            </div>
          </header>

          {organizationsEnabled && (
            <OrganizationStep
              currentUser={this.props.currentUser}
              finished={this.state.organization != null}
              onContinue={this.handleOrganizationDone}
              onOpen={this.handleOrganizationOpen}
              open={step === 'organization'}
              stepNumber={stepNumber++}
            />
          )}

          <TokenStep
            currentUser={this.props.currentUser}
            finished={this.state.token != null}
            onContinue={this.handleTokenDone}
            onOpen={this.handleTokenOpen}
            open={step === 'token'}
            stepNumber={stepNumber++}
          />

          <AnalysisStep
            onFinish={this.handleFinish}
            onReset={this.handleReset}
            organization={this.state.organization}
            open={step === 'analysis'}
            sonarCloud={sonarCloud}
            stepNumber={stepNumber}
            token={token}
          />

          {this.state.finished &&
            !this.state.skipping &&
            (this.state.projectKey ? (
              <ProjectWatcher
                onFinish={this.finishOnboarding}
                onTimeout={this.handleTimeout}
                projectKey={this.state.projectKey}
              />
            ) : (
              <footer className="text-right">
                <a className="button" href="#" onClick={this.handleSkipClick}>
                  {translate('tutorials.finish')}
                </a>
              </footer>
            ))}
        </div>
      </div>
    );
  }
}
