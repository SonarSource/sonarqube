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
import PluginDescription from './PluginDescription';
import PluginLicense from './PluginLicense';
import PluginOrganization from './PluginOrganization';
import PluginStatus from './PluginStatus';
import PluginUpdates from './PluginUpdates';
import PluginUrls from './PluginUrls';
import { PluginInstalled as IPluginInstalled } from '../../../api/plugins';
import { translate } from '../../../helpers/l10n';
import { Query } from '../utils';

interface Props {
  plugin: IPluginInstalled;
  readOnly: boolean;
  refreshPending: () => void;
  status?: string;
  updateQuery: (newQuery: Partial<Query>) => void;
}

export default function PluginInstalled({
  plugin,
  readOnly,
  refreshPending,
  status,
  updateQuery
}: Props) {
  return (
    <tr>
      <PluginDescription plugin={plugin} updateQuery={updateQuery} />
      <td className="text-top big-spacer-right">
        <ul>
          <li className="little-spacer-bottom">
            <strong className="js-plugin-installed-version little-spacer-right">
              {plugin.version}
            </strong>
            {translate('marketplace._installed')}
          </li>
          <PluginUpdates updates={plugin.updates} />
        </ul>
      </td>

      <td className="text-top width-20">
        <ul>
          <PluginUrls plugin={plugin} />
          <PluginLicense license={plugin.license} />
          <PluginOrganization plugin={plugin} />
        </ul>
      </td>

      {!readOnly && (
        <PluginStatus plugin={plugin} status={status} refreshPending={refreshPending} />
      )}
    </tr>
  );
}
