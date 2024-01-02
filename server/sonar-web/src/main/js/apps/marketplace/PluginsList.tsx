/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { sortBy } from 'lodash';
import * as React from 'react';
import { isAvailablePlugin, isInstalledPlugin, PendingPlugin, Plugin } from '../../types/plugins';
import PluginAvailable from './components/PluginAvailable';
import PluginInstalled from './components/PluginInstalled';

export interface PluginsListProps {
  plugins: Plugin[];
  pending: {
    installing: PendingPlugin[];
    updating: PendingPlugin[];
    removing: PendingPlugin[];
  };
  readOnly: boolean;
  refreshPending: () => void;
}

function getPluginStatus(plugin: Plugin, pending: PluginsListProps['pending']): string | undefined {
  const { installing, updating, removing } = pending;
  if (installing.find((p) => p.key === plugin.key)) {
    return 'installing';
  }
  if (updating.find((p) => p.key === plugin.key)) {
    return 'updating';
  }
  if (removing.find((p) => p.key === plugin.key)) {
    return 'removing';
  }
  return undefined;
}

export default function PluginsList(props: PluginsListProps) {
  const { pending, plugins, readOnly } = props;
  const installedPlugins = plugins.filter(isInstalledPlugin);
  return (
    <div className="boxed-group boxed-group-inner" id="marketplace-plugins">
      <ul>
        {sortBy(plugins, ({ name }) => name).map((plugin) => (
          <li className="panel panel-vertical" key={plugin.key}>
            <table className="marketplace-plugin-table">
              <tbody>
                {isInstalledPlugin(plugin) && (
                  <PluginInstalled
                    plugin={plugin}
                    readOnly={readOnly}
                    refreshPending={props.refreshPending}
                    status={getPluginStatus(plugin, pending)}
                  />
                )}

                {isAvailablePlugin(plugin) && (
                  <PluginAvailable
                    installedPlugins={installedPlugins}
                    plugin={plugin}
                    readOnly={readOnly}
                    refreshPending={props.refreshPending}
                    status={getPluginStatus(plugin, pending)}
                  />
                )}
              </tbody>
            </table>
          </li>
        ))}
      </ul>
    </div>
  );
}
