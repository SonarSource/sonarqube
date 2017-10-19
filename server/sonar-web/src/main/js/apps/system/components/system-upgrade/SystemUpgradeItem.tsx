/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import DateFormatter from '../../../../components/intl/DateFormatter';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';
import { SystemUpgrade } from '../../../../api/system';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  badge?: string;
  systemUpgrades: SystemUpgrade[];
}

export default function SystemUpgradeItem({ badge, systemUpgrades }: Props) {
  const lastUpgrade = systemUpgrades[0];
  return (
    <div className="system-upgrade-version">
      {badge && <span className="spacer-bottom badge badge-secondary">{badge}</span>}
      <p>
        <FormattedMessage
          defaultMessage={translate('system.version_is_availble')}
          id="system.version_is_availble"
          values={{ version: <b>SonarQube {lastUpgrade.version}</b> }}
        />
      </p>
      <p className="spacer-top">{lastUpgrade.description}</p>
      <div className="big-spacer-top">
        <DateFormatter date={lastUpgrade.releaseDate} long={true}>
          {formattedDate => (
            <span>{translateWithParameters('system.released_x', formattedDate)}</span>
          )}
        </DateFormatter>
        <a className="spacer-left" href={lastUpgrade.changeLogUrl} target="_blank">
          {translate('system.release_notes')}
        </a>
      </div>
      <SystemUpgradeIntermediate className="spacer-top" upgrades={systemUpgrades.slice(1)} />
      <div className="big-spacer-top">
        <a
          className="button"
          download={`sonarqube-${lastUpgrade.version}.zip`}
          href={lastUpgrade.downloadUrl}
          target="blank">
          {translateWithParameters('system.download_x', lastUpgrade.version)}
        </a>
        <a
          className="spacer-left"
          href="https://redirect.sonarsource.com/doc/upgrading.html"
          target="_blank">
          {translate('system.how_to_upgrade')}
        </a>
      </div>
    </div>
  );
}
