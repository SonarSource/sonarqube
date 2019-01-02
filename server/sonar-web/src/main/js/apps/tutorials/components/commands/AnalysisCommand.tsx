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
import JavaMaven from './JavaMaven';
import JavaGradle from './JavaGradle';
import DotNet from './DotNet';
import Msvc from './Msvc';
import ClangGCC from './ClangGCC';
import Other from './Other';
import { getHostUrl } from '../../../../helpers/urls';
import { LanguageConfig } from '../../utils';

interface Props {
  component?: T.Component;
  organization?: string;
  languageConfig: LanguageConfig;
  small?: boolean;
  token?: string;
}

export default class AnalysisCommand extends React.PureComponent<Props> {
  getProjectKey = ({ component, languageConfig } = this.props) => {
    return (component && component.key) || languageConfig.projectKey;
  };

  renderCommandForMaven = () => {
    const { component, token } = this.props;
    if (!token) {
      return null;
    }
    return (
      <JavaMaven
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={component && component.key}
        token={token}
      />
    );
  };

  renderCommandForGradle = () => {
    const { component, token } = this.props;
    if (!token) {
      return null;
    }
    return (
      <JavaGradle
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={component && component.key}
        token={token}
      />
    );
  };

  renderCommandForDotNet = () => {
    const { small, token } = this.props;
    const projectKey = this.getProjectKey();
    if (!projectKey || !token) {
      return null;
    }
    return (
      <DotNet
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={projectKey}
        small={small}
        token={token}
      />
    );
  };

  renderCommandForMSVC = () => {
    const { small, token } = this.props;
    const projectKey = this.getProjectKey();
    if (!projectKey || !token) {
      return null;
    }
    return (
      <Msvc
        host={getHostUrl()}
        organization={this.props.organization}
        projectKey={projectKey}
        small={small}
        token={token}
      />
    );
  };

  renderCommandForClangGCC = () => {
    const { languageConfig, small, token } = this.props;
    const projectKey = this.getProjectKey();
    if (!languageConfig || !projectKey || !languageConfig.os || !token) {
      return null;
    }
    return (
      <ClangGCC
        host={getHostUrl()}
        organization={this.props.organization}
        os={languageConfig.os}
        projectKey={projectKey}
        small={small}
        token={token}
      />
    );
  };

  renderCommandForOther = () => {
    const { languageConfig, token } = this.props;
    const projectKey = this.getProjectKey();
    if (!languageConfig || !projectKey || !languageConfig.os || !token) {
      return null;
    }
    return (
      <Other
        host={getHostUrl()}
        organization={this.props.organization}
        os={languageConfig.os}
        projectKey={projectKey}
        token={token}
      />
    );
  };

  render() {
    const { languageConfig } = this.props;

    if (languageConfig.language === 'java') {
      return languageConfig.javaBuild === 'maven'
        ? this.renderCommandForMaven()
        : this.renderCommandForGradle();
    } else if (languageConfig.language === 'dotnet') {
      return this.renderCommandForDotNet();
    } else if (languageConfig.language === 'c-family') {
      return languageConfig.cFamilyCompiler === 'msvc'
        ? this.renderCommandForMSVC()
        : this.renderCommandForClangGCC();
    } else {
      return this.renderCommandForOther();
    }
  }
}
