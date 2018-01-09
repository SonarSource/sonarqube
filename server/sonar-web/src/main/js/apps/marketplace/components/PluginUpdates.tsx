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
import PluginUpdateItem from './PluginUpdateItem';
import { Update } from '../../../api/plugins';
import { translate } from '../../../helpers/l10n';

interface Props {
  updates?: Update[];
}

export default function PluginUpdates({ updates }: Props) {
  if (!updates || updates.length <= 0) {
    return null;
  }
  return (
    <li className="spacer-top">
      <strong>{translate('marketplace.updates')}:</strong>
      <ul className="little-spacer-top">
        {updates.map(
          update =>
            update.release ? (
              <PluginUpdateItem
                key={update.release.version}
                release={update.release}
                update={update}
              />
            ) : null
        )}
      </ul>
    </li>
  );
}
