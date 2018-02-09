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
import Step from './Step';
import LanguageStep from './LanguageStep';
/*:: import type { Result } from './LanguageStep'; */
import JavaMaven from './commands/JavaMaven';
import JavaGradle from './commands/JavaGradle';
import DotNet from './commands/DotNet';
import Msvc from './commands/Msvc';
import ClangGCC from './commands/ClangGCC';
import Other from './commands/Other';
import { translate } from '../../../helpers/l10n';
import { getHostUrl } from '../../../helpers/urls';

/*::
type Props = {|
  onFinish: (projectKey?: string) => void,
  onReset: () => void,
  open: boolean,
  organization?: string,
  sonarCloud: boolean,
  stepNumber: number,
  token: string
|};
*/

/*::
type State = {
  result?: Result
};
*/

export default class AnalysisStep extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {};

  handleLanguageSelect = (result /*: Result | void */) => {
    this.setState({ result });
    const projectKey = result && result.language !== 'java' ? result.projectKey : undefined;
    this.props.onFinish(projectKey);
  };

  handleLanguageReset = () => {
    this.setState({ result: undefined });
    this.props.onReset();
  };

  renderForm = () => {
    return (
      <div className="boxed-group-inner">
        <div className="flex-columns">
          <div className="flex-column flex-column-half bordered-right">
            <LanguageStep
              onDone={this.handleLanguageSelect}
              onReset={this.handleLanguageReset}
              organization={this.props.organization}
              sonarCloud={this.props.sonarCloud}
            />
          </div>
          <div className="flex-column flex-column-half">{this.renderCommand()}</div>
        </div>
      </div>
    );
  };

  renderFormattedCommand = (...lines /*: Array<string> */) => (
    // keep this "useless" concatentation for the readability reason
    // eslint-disable-next-line no-useless-concat
    <pre>{lines.join(' ' + '\\' + '\n' + '  ')}</pre>
  );

  renderCommand = () => {
    const { result } = this.state;

    if (!result) {
      return null;
    }

    if (result.language === 'java') {
      return result.javaBuild === 'maven'
        ? this.renderCommandForMaven()
        : this.renderCommandForGradle();
    } else if (result.language === 'dotnet') {
      return this.renderCommandForDotNet();
    } else if (result.language === 'c-family') {
      return result.cFamilyCompiler === 'msvc'
        ? this.renderCommandForMSVC()
        : this.renderCommandForClangGCC();
    } else {
      return this.renderCommandForOther();
    }
  };

  renderCommandForMaven = () => (
    <JavaMaven
      host={getHostUrl()}
      organization={this.props.organization}
      token={this.props.token}
    />
  );

  renderCommandForGradle = () => (
    <JavaGradle
      host={getHostUrl()}
      organization={this.props.organization}
      token={this.props.token}
    />
  );

  renderCommandForDotNet = () => {
    return (
      <DotNet
        host={getHostUrl()}
        organization={this.props.organization}
        // $FlowFixMe
        projectKey={this.state.result.projectKey}
        token={this.props.token}
      />
    );
  };

  renderCommandForMSVC = () => {
    return (
      <Msvc
        host={getHostUrl()}
        organization={this.props.organization}
        // $FlowFixMe
        projectKey={this.state.result.projectKey}
        token={this.props.token}
      />
    );
  };

  renderCommandForClangGCC = () => (
    <ClangGCC
      host={getHostUrl()}
      organization={this.props.organization}
      // $FlowFixMe
      os={this.state.result.os}
      // $FlowFixMe
      projectKey={this.state.result.projectKey}
      token={this.props.token}
    />
  );

  renderCommandForOther = () => (
    <Other
      host={getHostUrl()}
      organization={this.props.organization}
      // $FlowFixMe
      os={this.state.result.os}
      // $FlowFixMe
      projectKey={this.state.result.projectKey}
      token={this.props.token}
    />
  );

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
