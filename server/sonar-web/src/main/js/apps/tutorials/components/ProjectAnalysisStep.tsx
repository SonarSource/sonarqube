/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { LanguageConfig } from '../utils';
import AnalysisCommand from './commands/AnalysisCommand';
import LanguageForm from './LanguageForm';
import Step from './Step';

interface Props {
  component?: T.Component;
  displayRowLayout?: boolean;
  onFinish?: (projectKey?: string) => void;
  onReset?: VoidFunction;
  open: boolean;
  organization?: string;
  stepNumber: number;
  token?: string;
}

interface State {
  config?: LanguageConfig;
}

export function getProjectKey(config?: LanguageConfig, component?: T.Component) {
  return (component && component.key) || (config && config.projectKey);
}

export default class ProjectAnalysisStep extends React.PureComponent<Props, State> {
  state: State = {};

  handleLanguageSelect = (config: LanguageConfig) => {
    this.setState({ config });
    if (this.props.onFinish) {
      const projectKey = config.language !== 'java' ? getProjectKey(config) : undefined;
      this.props.onFinish(projectKey);
    }
  };

  handleLanguageReset = () => {
    this.setState({ config: undefined });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  renderForm = () => {
    const languageComponent = (
      <LanguageForm
        component={this.props.component}
        onDone={this.handleLanguageSelect}
        onReset={this.handleLanguageReset}
        organization={this.props.organization}
      />
    );
    const analysisComponent = this.state.config && (
      <AnalysisCommand
        component={this.props.component}
        languageConfig={this.state.config}
        organization={this.props.organization}
        small={true}
        token={this.props.token}
      />
    );

    if (this.props.displayRowLayout) {
      return (
        <div className="boxed-group-inner">
          <div className="display-flex-column">
            {languageComponent}
            {analysisComponent && <div className="huge-spacer-top">{analysisComponent}</div>}
          </div>
        </div>
      );
    }

    return (
      <div className="boxed-group-inner">
        <div className="flex-columns">
          <div className="flex-column flex-column-half bordered-right">{languageComponent}</div>
          <div className="flex-column flex-column-half">{analysisComponent}</div>
        </div>
      </div>
    );
  };

  renderResult = () => null;

  render() {
    return (
      <Step
        finished={false}
        onOpen={() => {}}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle={translate('onboarding.analysis.header')}
      />
    );
  }
}
