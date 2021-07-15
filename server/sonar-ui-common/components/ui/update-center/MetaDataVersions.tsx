/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import MetaDataVersion from './MetaDataVersion';
import { MetaDataVersionInformation } from './update-center-metadata';

interface Props {
  versions: MetaDataVersionInformation[];
}

interface State {
  collapsed: boolean;
}

export default class MetaDataVersions extends React.Component<Props, State> {
  state: State = {
    collapsed: true,
  };

  componentDidUpdate(prevProps: Props) {
    if (prevProps.versions !== this.props.versions) {
      this.setState({ collapsed: true });
    }
  }

  handleClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState(({ collapsed }) => ({ collapsed: !collapsed }));
  };

  render() {
    const { versions } = this.props;
    const { collapsed } = this.state;

    const archivedVersions = versions.filter((version) => version.archived);
    const currentVersions = versions.filter((version) => !version.archived);

    return (
      <div className="update-center-meta-data-versions">
        {archivedVersions.length > 0 && (
          <button
            className="update-center-meta-data-versions-show-more"
            onClick={this.handleClick}
            type="button">
            {collapsed ? 'Show more versions' : 'Show fewer versions'}
          </button>
        )}

        {currentVersions.map((versionInformation) => (
          <MetaDataVersion
            key={versionInformation.version}
            versionInformation={versionInformation}
          />
        ))}

        {!collapsed &&
          archivedVersions.map((archivedVersionInformation) => (
            <MetaDataVersion
              key={archivedVersionInformation.version}
              versionInformation={archivedVersionInformation}
            />
          ))}
      </div>
    );
  }
}
