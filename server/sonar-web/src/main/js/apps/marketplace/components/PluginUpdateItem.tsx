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
import PluginChangeLogButton from './PluginChangeLogButton';
import Tooltip from '../../../components/controls/Tooltip';
import { Release, Update } from '../../../api/plugins';
import { translate } from '../../../helpers/l10n';

interface Props {
  update: Update;
  release: Release;
}

interface State {
  changelogOpen: boolean;
}

export default class PluginUpdateItem extends React.PureComponent<Props, State> {
  state: State = { changelogOpen: false };

  handleChangelogClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.toggleChangelog();
  };

  toggleChangelog = (show?: boolean) => {
    if (show !== undefined) {
      this.setState({ changelogOpen: show });
    } else {
      this.setState(state => ({ changelogOpen: !state.changelogOpen }));
    }
  };

  render() {
    const { release, update } = this.props;
    return (
      <li className="display-flex-row little-spacer-bottom" key={release.version}>
        <div className="pull-left spacer-right">
          {update.status === 'COMPATIBLE' ? (
            <span className="js-update-version badge badge-success">{release.version}</span>
          ) : (
            <Tooltip overlay={translate('marketplace.update_status', update.status)}>
              <span className="js-update-version badge badge-warning">{release.version}</span>
            </Tooltip>
          )}
        </div>
        <div>
          {release.description}
          <PluginChangeLogButton release={release} update={update} />
        </div>
      </li>
    );
  }
}
