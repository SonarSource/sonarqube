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
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import ProjectWatcher from './ProjectWatcher';
import ProjectAnalysisStep from '../components/ProjectAnalysisStep';
import OrganizationStep from '../components/OrganizationStep';
import TokenStep from '../components/TokenStep';
import handleRequiredAuthentication from '../../../app/utils/handleRequiredAuthentication';
import { getCurrentUser, areThereCustomOrganizations, Store } from '../../../store/rootReducer';
import { CurrentUser } from '../../../app/types';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { getProjectUrl } from '../../../helpers/urls';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import { isLoggedIn } from '../../../helpers/users';
import '../styles.css';

interface OwnProps {
  automatic?: boolean;
  onFinish: () => void;
}

interface StateProps {
  currentUser: CurrentUser;
  organizationsEnabled?: boolean;
}

type Props = OwnProps & StateProps;

interface State {
  finished: boolean;
  organization?: string;
  projectKey?: string;
  step: string;
  token?: string;
}

export class ProjectOnboarding extends React.PureComponent<Props, State> {
  mounted = false;
  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      finished: false,
      step: props.organizationsEnabled ? 'organization' : 'token'
    };
  }

  componentDidMount() {
    this.mounted = true;

    // useCapture = true to receive the event before inputs
    window.addEventListener('keydown', this.onKeyDown, true);

    if (!isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.onKeyDown, true);
    this.mounted = false;
  }

  onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      this.finishOnboarding();
    }
  };

  finishOnboarding = () => {
    this.props.onFinish();
    if (this.state.projectKey) {
      this.context.router.push(getProjectUrl(this.state.projectKey));
    }
  };

  handleTimeout = () => {
    // unset `projectKey` to display a generic "Finish this tutorial" button
    this.setState({ projectKey: undefined });
  };

  handleTokenDone = (token: string) => {
    this.setState({ step: 'analysis', token });
  };

  handleOrganizationDone = (organization: string) => {
    this.setState({ organization, step: 'token' });
  };

  handleTokenOpen = () => this.setState({ step: 'token' });

  handleOrganizationOpen = () => this.setState({ step: 'organization' });

  handleFinish = (projectKey?: string) => this.setState({ finished: true, projectKey });

  handleReset = () => this.setState({ finished: false, projectKey: undefined });

  render() {
    const { automatic, currentUser, organizationsEnabled } = this.props;
    if (!isLoggedIn(currentUser)) {
      return null;
    }

    const { finished, projectKey, step, token } = this.state;
    const header = translate('onboarding.project.header');
    let stepNumber = 1;

    return (
      <>
        <Helmet title={header} titleTemplate="%s" />
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body modal-container">
          <p className="spacer-top big-spacer-bottom">
            {translateWithParameters(
              'onboarding.project.header.description',
              organizationsEnabled ? 3 : 2
            )}
          </p>
          {organizationsEnabled && (
            <OrganizationStep
              currentUser={currentUser}
              finished={this.state.organization != null}
              onContinue={this.handleOrganizationDone}
              onOpen={this.handleOrganizationOpen}
              open={step === 'organization'}
              stepNumber={stepNumber++}
            />
          )}

          <TokenStep
            currentUser={currentUser}
            finished={this.state.token != null}
            onContinue={this.handleTokenDone}
            onOpen={this.handleTokenOpen}
            open={step === 'token'}
            stepNumber={stepNumber++}
          />

          <ProjectAnalysisStep
            onFinish={this.handleFinish}
            onReset={this.handleReset}
            open={step === 'analysis'}
            organization={this.state.organization}
            stepNumber={stepNumber}
            token={token}
          />
        </div>
        <footer className="modal-foot">
          <ResetButtonLink className="js-skip" onClick={this.finishOnboarding}>
            {(finished && translate('tutorials.finish')) ||
              (automatic ? translate('tutorials.skip') : translate('close'))}
          </ResetButtonLink>
          {finished && projectKey ? (
            <ProjectWatcher
              onFinish={this.finishOnboarding}
              onTimeout={this.handleTimeout}
              projectKey={projectKey}
            />
          ) : (
            <span className="pull-left note">
              {translate(
                isSonarCloud()
                  ? 'tutorials.find_tutorial_back_in_plus'
                  : 'tutorials.find_tutorial_back_in_help'
              )}
            </span>
          )}
        </footer>
      </>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  return {
    currentUser: getCurrentUser(state),
    organizationsEnabled: areThereCustomOrganizations(state)
  };
};

export default connect(mapStateToProps)(ProjectOnboarding);
