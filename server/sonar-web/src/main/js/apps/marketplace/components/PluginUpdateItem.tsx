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

import { Badge, ListItem } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { Release, Update } from '../../../types/plugins';
import PluginChangeLogButton from './PluginChangeLogButton';

interface Props {
  pluginName: string;
  release: Release;
  update: Update;
}

export default function PluginUpdateItem({ release, update, pluginName }: Readonly<Props>) {
  return (
    <ListItem className="sw-flex sw-items-center" key={release.version}>
      <div className="sw-mr-2">
        {update.status === 'COMPATIBLE' ? (
          <Badge variant="new">{release.version}</Badge>
        ) : (
          <Tooltip content={translate('marketplace.update_status', update.status)}>
            <span>
              <Badge>{release.version}</Badge>
            </span>
          </Tooltip>
        )}
      </div>
      <div>
        {release.description}
        <PluginChangeLogButton pluginName={pluginName} release={release} update={update} />
      </div>
    </ListItem>
  );
}
