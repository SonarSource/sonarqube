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
import DateFormatter from '../../../components/intl/DateFormatter';
import Tooltip from '../../../components/controls/Tooltip';
import { Release, Update } from '../../../api/plugins';
import { translate } from '../../../helpers/l10n';

interface Props {
  release: Release;
  update: Update;
}

export default function PluginChangeLogItem({ release, update }: Props) {
  return (
    <li className="big-spacer-bottom">
      <div className="little-spacer-bottom">
        {update.status === 'COMPATIBLE' || !update.status ? (
          <span className="js-plugin-changelog-version badge badge-success spacer-right">
            {release.version}
          </span>
        ) : (
          <Tooltip overlay={translate('marketplace.update_status', update.status)}>
            <span className="js-plugin-changelog-version badge badge-warning spacer-right">
              {release.version}
            </span>
          </Tooltip>
        )}
        <span className="js-plugin-changelog-date note spacer-right">
          <DateFormatter date={release.date} />
        </span>
        {release.changeLogUrl && (
          <a className="js-plugin-changelog-link" href={release.changeLogUrl} target="_blank">
            {translate('marketplace.release_notes')}
          </a>
        )}
      </div>
      <div className="js-plugin-changelog-description">{release.description}</div>
    </li>
  );
}
