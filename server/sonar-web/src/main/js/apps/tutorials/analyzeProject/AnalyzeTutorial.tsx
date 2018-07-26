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
import AnalyzeTutorialSuggestion from './AnalyzeTutorialSuggestion';
import ProjectAnalysisStep from '../components/ProjectAnalysisStep';
import TokenStep from '../components/TokenStep';
import { Component, LoggedInUser } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

enum Steps {
  ANALYSIS,
  TOKEN
}

interface Props {
  component: Component;
  currentUser: LoggedInUser;
}

interface State {
  step: Steps;
  token?: string;
}

export default class AnalyzeTutorial extends React.PureComponent<Props, State> {
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
    let stepNumber = 1;

    const almId = component.almId || currentUser.externalProvider;
    const showTutorial = almId !== 'microsoft';
    return (
      <>
        <div className="page-header big-spacer-bottom">
          <h1 className="page-title">{translate('onboarding.project_analysis.header')}</h1>
          <p className="page-description">{translate('onboarding.project_analysis.description')}</p>
        </div>

        <AnalyzeTutorialSuggestion almId={almId} />

        {showTutorial && (
          <>
            <TokenStep
              currentUser={currentUser}
              finished={Boolean(this.state.token)}
              onContinue={this.handleTokenDone}
              onOpen={this.handleTokenOpen}
              open={step === Steps.TOKEN}
              stepNumber={stepNumber++}
            />

            <ProjectAnalysisStep
              component={component}
              displayRowLayout={true}
              open={step === Steps.ANALYSIS}
              organization={component.organization}
              stepNumber={stepNumber++}
              token={token}
            />
          </>
        )}
      </>
    );
  }
}
