/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import BackIcon from 'sonar-ui-common/components/icons/BackIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isVSTS } from '../../../helpers/almIntegrations';
import InstanceMessage from '../../common/InstanceMessage';
import ProjectAnalysisStep from './ProjectAnalysisStep';
import TokenStep from './TokenStep';

export enum Steps {
  ANALYSIS,
  TOKEN
}

interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
  onBack?: () => void;
}

interface State {
  step: Steps;
  token?: string;
}

export default class ManualTutorial extends React.PureComponent<Props, State> {
  state: State = { step: Steps.TOKEN };

  handleTokenDone = (token: string) => {
    this.setState({ step: Steps.ANALYSIS, token });
  };

  handleTokenOpen = () => {
    this.setState({ step: Steps.TOKEN });
  };

  render() {
    const { component, currentUser } = this.props;
    const { step, token } = this.state;

    const almKey = (component.alm && component.alm.key) || currentUser.externalProvider;
    return (
      <>
        <div className="page-header big-spacer-bottom">
          <h1 className="page-title">
            {this.props.onBack !== undefined && (
              <Tooltip overlay={translate('onboarding.tutorial.return_to_list')}>
                <a
                  aria-label={translate('onboarding.tutorial.return_to_list')}
                  className="link-no-underline big-spacer-right"
                  onClick={this.props.onBack}>
                  <BackIcon />
                </a>
              </Tooltip>
            )}
            {translate('onboarding.project_analysis.header')}
          </h1>
          <p className="page-description">
            <InstanceMessage message={translate('onboarding.project_analysis.description')} />
          </p>
        </div>

        {!isVSTS(almKey) && (
          <>
            <TokenStep
              currentUser={currentUser}
              finished={Boolean(this.state.token)}
              initialTokenName={`Analyze "${component.name}"`}
              onContinue={this.handleTokenDone}
              onOpen={this.handleTokenOpen}
              open={step === Steps.TOKEN}
              stepNumber={1}
            />

            <ProjectAnalysisStep
              component={component}
              displayRowLayout={true}
              open={step === Steps.ANALYSIS}
              stepNumber={2}
              token={token}
            />
          </>
        )}
      </>
    );
  }
}
