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
import { Card, Table, TableRow } from '~design-system';
import { translate } from '../../helpers/l10n';
import { PendingPlugin, Plugin, isAvailablePlugin, isInstalledPlugin } from '../../types/plugins';
import PluginAvailable from './components/PluginAvailable';
import PluginInstalled from './components/PluginInstalled';

export interface PluginsListProps {
  pending: {
    installing: PendingPlugin[];
    removing: PendingPlugin[];
    updating: PendingPlugin[];
  };
  plugins: Plugin[];
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

export default function PluginsList(props: Readonly<PluginsListProps>) {
  const { pending, plugins, readOnly } = props;
  const installedPlugins = plugins.filter(isInstalledPlugin);

  const columns = readOnly ? ['25%', 'auto', '20%'] : ['25%', 'auto', '20%', '20%'];

  return (
    <Card id="marketplace-plugins">
      <Table
        aria-label={translate('marketplace.page.plugins')}
        columnCount={columns.length}
        columnWidths={columns}
      >
        {sortBy(plugins, ({ name }) => name).map((plugin) => (
          <TableRow key={plugin.key}>
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
          </TableRow>
        ))}
      </Table>
    </Card>
  );
}
