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
import Config from './components/Config';
import PullRequestWidget from './components/PullRequestWidget';
import RepoWidget from './components/RepoWidget';
import { getProjectKey, getPullRequestContext, getDisabled } from './utils';
import { WidgetType, AppContext } from './types';

interface Props {
  context: AppContext;
  page: WidgetType;
}

interface State {
  disabled: boolean;
  projectKey: string | undefined;
}

export default class App extends React.PureComponent<Props, State> {
  state: State = { disabled: getDisabled(), projectKey: getProjectKey() };

  handleUpdateDisabled = (disabled: boolean) => {
    this.setState({ disabled });
  };

  handleUpdateProjectKey = (projectKey: string) => {
    this.setState({ projectKey });
  };

  render() {
    const { context, page } = this.props;
    const { disabled, projectKey } = this.state;

    if (page === 'repository-config') {
      return (
        <Config
          context={context}
          disabled={disabled}
          projectKey={projectKey}
          updateDisabled={this.handleUpdateDisabled}
          updateProjectKey={this.handleUpdateProjectKey}
        />
      );
    }

    if (page === 'pullrequest-code-quality') {
      return <PullRequestWidget context={getPullRequestContext()} />;
    }

    return <RepoWidget context={context} />;
  }
}
