/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { LightLabel, PageContentFontWrapper, Title } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import ProjectAnalysisStep from './ProjectAnalysisStep';
import TokenStep from './TokenStep';

export enum Steps {
  ANALYSIS,
  TOKEN,
}

interface Props {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  isLocal?: boolean;
}

interface State {
  step: Steps;
  token?: string;
}

export default class OtherTutorial extends React.PureComponent<Props, State> {
  state: State = { step: Steps.TOKEN };

  handleTokenDone = (token: string) => {
    this.setState({ step: Steps.ANALYSIS, token });
  };

  handleTokenOpen = () => {
    this.setState({ step: Steps.TOKEN });
  };

  render() {
    const { component, baseUrl, currentUser, isLocal = false } = this.props;
    const { step, token } = this.state;

    return (
      <PageContentFontWrapper className="sw-typo-default">
        <div className="sw-mb-4">
          <Title>{translate('onboarding.project_analysis.header')} </Title>
          <LightLabel>{translate('onboarding.project_analysis.description')}</LightLabel>
        </div>

        <TokenStep
          currentUser={currentUser}
          projectKey={component.key}
          finished={Boolean(this.state.token)}
          initialTokenName={`Analyze "${component.name}"`}
          onContinue={this.handleTokenDone}
          onOpen={this.handleTokenOpen}
          open={step === Steps.TOKEN}
          stepNumber={1}
        />

        <ProjectAnalysisStep
          component={component}
          baseUrl={baseUrl}
          isLocal={isLocal}
          open={step === Steps.ANALYSIS}
          token={token}
          stepNumber={2}
        />
      </PageContentFontWrapper>
    );
  }
}
