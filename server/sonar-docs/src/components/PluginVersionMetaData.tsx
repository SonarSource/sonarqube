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
import classNames from 'classnames';
import * as React from 'react';
import { PluginVersionInfo } from '../@types/types';

interface Props {
  versions: PluginVersionInfo[];
}

interface State {
  collapsed: boolean;
}

export default class PluginVersionMetaData extends React.Component<Props, State> {
  state: State = {
    collapsed: true
  };

  handleClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState(({ collapsed }) => ({ collapsed: !collapsed }));
  };

  renderVersion = ({
    archived,
    changeLogUrl,
    compatibility,
    date,
    description,
    downloadURL,
    version
  }: PluginVersionInfo) => {
    return (
      <div
        className={classNames('plugin-meta-data-version', {
          'plugin-meta-data-version-archived': archived
        })}
        key={version}>
        <div className="plugin-meta-data-version-version">{version}</div>

        <div className="plugin-meta-data-version-release-info">
          {date && <time className="plugin-meta-data-version-release-date">{date}</time>}

          {compatibility && (
            <span className="plugin-meta-data-version-compatibility">{compatibility}</span>
          )}
        </div>

        {description && (
          <div className="plugin-meta-data-version-release-description">{description}</div>
        )}

        {(downloadURL || changeLogUrl) && (
          <div className="plugin-meta-data-version-release-links">
            {downloadURL && (
              <span className="plugin-meta-data-version-download">
                <a href={downloadURL} rel="noopener noreferrer" target="_blank">
                  Download
                </a>
              </span>
            )}

            {changeLogUrl && (
              <span className="plugin-meta-data-version-release-notes">
                <a href={changeLogUrl} rel="noopener noreferrer" target="_blank">
                  Release notes
                </a>
              </span>
            )}
          </div>
        )}
      </div>
    );
  };

  render() {
    const { versions } = this.props;
    const { collapsed } = this.state;

    const archivedVersions = versions.filter(version => version.archived);
    const currentVersions = versions.filter(version => !version.archived);
    return (
      <div className="plugin-meta-data-versions">
        {archivedVersions.length > 0 && (
          <button
            className="plugin-meta-data-versions-show-more"
            onClick={this.handleClick}
            type="button">
            {collapsed ? 'Show more versions' : 'Show fewer version'}
          </button>
        )}

        {currentVersions.map(version => this.renderVersion(version))}

        {!collapsed && archivedVersions.map(version => this.renderVersion(version))}
      </div>
    );
  }
}
