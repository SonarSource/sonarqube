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
import SonarCloudIcon from './SonarCloudIcon';
import { getRepoSettingsUrl, isRepoAdmin } from '../utils';

interface State {
  settingsUrl?: string;
}

export default class RepoWidgetNotConfigured extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    getRepoSettingsUrl().then(
      settingsUrl => {
        if (this.mounted) {
          this.setState({ settingsUrl });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  render() {
    const { settingsUrl } = this.state;
    const settingsLink = settingsUrl ? (
      <a href={settingsUrl} rel="noopener noreferrer" target="_parent">
        repository settings
      </a>
    ) : (
      'repository settings'
    );
    return (
      <>
        <div className="project-card-header">
          <div className="project-card-header-inner">
            <SonarCloudIcon size={24} />
            <h4 className="spacer-left">Code Quality</h4>
          </div>
        </div>
        <div className="spacer-top">
          {isRepoAdmin() ? (
            <>
              You have to link your repository with an existing SonarCloud project. Go to your{' '}
              {settingsLink}.
            </>
          ) : (
            'Contact a repository administrator to link the repository with a SonarCloud project.'
          )}
        </div>
      </>
    );
  }
}
