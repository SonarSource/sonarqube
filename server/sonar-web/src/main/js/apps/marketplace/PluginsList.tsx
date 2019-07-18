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
import { Plugin, PluginPending } from '../../api/plugins';
import PluginAvailable from './components/PluginAvailable';
import PluginInstalled from './components/PluginInstalled';
import { isPluginAvailable, isPluginInstalled } from './utils';

interface Props {
  plugins: Plugin[];
  pending: {
    installing: PluginPending[];
    updating: PluginPending[];
    removing: PluginPending[];
  };
  readOnly: boolean;
  refreshPending: () => void;
}

export default class PluginsList extends React.PureComponent<Props> {
  getPluginStatus = (plugin: Plugin): string | undefined => {
    const { installing, updating, removing } = this.props.pending;
    if (installing.find(p => p.key === plugin.key)) {
      return 'installing';
    }
    if (updating.find(p => p.key === plugin.key)) {
      return 'updating';
    }
    if (removing.find(p => p.key === plugin.key)) {
      return 'removing';
    }
    return undefined;
  };

  renderPlugin = (plugin: Plugin) => {
    const status = this.getPluginStatus(plugin);
    if (isPluginInstalled(plugin)) {
      return (
        <PluginInstalled
          plugin={plugin}
          readOnly={this.props.readOnly}
          refreshPending={this.props.refreshPending}
          status={status}
        />
      );
    }
    if (isPluginAvailable(plugin)) {
      return (
        <PluginAvailable
          plugin={plugin}
          readOnly={this.props.readOnly}
          refreshPending={this.props.refreshPending}
          status={status}
        />
      );
    }
    return null;
  };

  render() {
    return (
      <div className="boxed-group boxed-group-inner" id="marketplace-plugins">
        <ul>
          {this.props.plugins.map(plugin => (
            <li className="panel panel-vertical" key={plugin.key}>
              <table className="marketplace-plugin-table">
                <tbody>{this.renderPlugin(plugin)}</tbody>
              </table>
            </li>
          ))}
        </ul>
      </div>
    );
  }
}
