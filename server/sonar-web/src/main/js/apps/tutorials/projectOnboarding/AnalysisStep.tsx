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
import Step from './Step';
import LanguageStep, { Result } from './LanguageStep';
import JavaMaven from './commands/JavaMaven';
import JavaGradle from './commands/JavaGradle';
import DotNet from './commands/DotNet';
import Msvc from './commands/Msvc';
import ClangGCC from './commands/ClangGCC';
import Other from './commands/Other';
import { translate } from '../../../helpers/l10n';
import { getHostUrl } from '../../../helpers/urls';

interface Props {
  onFinish: (projectKey?: string) => void;
  onReset: () => void;
  open: boolean;
  organization?: string;
  stepNumber: number;
  token?: string;
}

interface State {
  result?: Result;
}

export default class AnalysisStep extends React.PureComponent<Props, State> {
  state: State = {};

  handleLanguageSelect = (result?: Result) => {
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
            />
          </div>
          <div className="flex-column flex-column-half">{this.renderCommand()}</div>
        </div>
      </div>
    );
  };

  renderFormattedCommand = (...lines: Array<string>) => (
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

  renderCommandForMaven = () => {
    const { token } = this.props;
    if (!token) {
      return null;
    }
    return <JavaMaven host={getHostUrl()} organization={this.props.organization} token={token} />;
  };

  renderCommandForGradle = () => {
    const { token } = this.props;
    if (!token) {
      return null;
    }
    return <JavaGradle host={getHostUrl()} organization={this.props.organization} token={token} />;
  };

  renderCommandForDotNet = () => {
    const { token } = this.props;
    const { result } = this.state;
    if (!result || !result.projectKey || !token) {
      return null;
    }
    return (
      <DotNet
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={result.projectKey}
        token={token}
      />
    );
  };

  renderCommandForMSVC = () => {
    const { token } = this.props;
    const { result } = this.state;
    if (!result || !result.projectKey || !token) {
      return null;
    }
    return (
      <Msvc
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={result.projectKey}
        token={token}
      />
    );
  };

  renderCommandForClangGCC = () => {
    const { token } = this.props;
    const { result } = this.state;
    if (!result || !result.projectKey || !result.os || !token) {
      return null;
    }
    return (
      <ClangGCC
        host={getHostUrl()}
        organization={this.props.organization}
        os={result.os}
        projectKey={result.projectKey}
        token={token}
      />
    );
  };

  renderCommandForOther = () => {
    const { token } = this.props;
    const { result } = this.state;
    if (!result || !result.projectKey || !result.os || !token) {
      return null;
    }
    return (
      <Other
        host={getHostUrl()}
        organization={this.props.organization}
        os={result.os}
        projectKey={result.projectKey}
        token={token}
      />
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
