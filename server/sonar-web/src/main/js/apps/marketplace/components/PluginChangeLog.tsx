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
import PluginChangeLogItem from './PluginChangeLogItem';
import { Release, Update } from '../../../api/plugins';
import { translate } from '../../../helpers/l10n';

export interface Props {
  release: Release;
  update: Update;
}

export default function PluginChangeLog({ release, update }: Props) {
  return (
    <div className="abs-width-300">
      <h6>{translate('changelog')}</h6>
      <ul className="js-plugin-changelog-list">
        {update.previousUpdates &&
          update.previousUpdates.map(previousUpdate =>
            previousUpdate.release ? (
              <PluginChangeLogItem
                key={previousUpdate.release.version}
                release={previousUpdate.release}
                update={previousUpdate}
              />
            ) : null
          )}
        <PluginChangeLogItem release={release} update={update} />
      </ul>
    </div>
  );
}
