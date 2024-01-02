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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  getEdition,
  getEditionDownloadFilename,
  getEditionDownloadUrl,
} from '../../helpers/editions';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { SystemUpgrade } from '../../types/system';
import DocLink from '../common/DocLink';
import Link from '../common/Link';
import DateFormatter from '../intl/DateFormatter';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';

export interface SystemUpgradeItemProps {
  edition: EditionKey | undefined;
  isLTSVersion: boolean;
  isPatch: boolean;
  systemUpgrades: SystemUpgrade[];
}

export default function SystemUpgradeItem(props: SystemUpgradeItemProps) {
  const { edition, isPatch, isLTSVersion, systemUpgrades } = props;
  const lastUpgrade = systemUpgrades[0];
  const downloadUrl = getEditionDownloadUrl(
    getEdition(edition || EditionKey.community),
    lastUpgrade,
  );
  let header = translate('system.latest_version');
  if (isLTSVersion) {
    header = translate('system.lts_version');
  } else if (isPatch) {
    header = translate('system.latest_patch');
  }

  return (
    <div className="system-upgrade-version it__upgrade-list-item">
      <h3 className="h1 spacer-bottom">
        <strong>{header}</strong>
        {!isPatch && (
          <Link
            className="spacer-left medium"
            to="https://www.sonarsource.com/products/sonarqube/whats-new/?referrer=sonarqube"
            target="_blank"
          >
            {translate('system.see_whats_new')}
          </Link>
        )}
      </h3>
      <p>
        <FormattedMessage
          defaultMessage={translate('system.version_is_availble')}
          id="system.version_is_availble"
          values={{ version: <b>SonarQube {lastUpgrade.version}</b> }}
        />
      </p>
      <p className="spacer-top">{lastUpgrade.description}</p>
      <div className="big-spacer-top">
        {lastUpgrade.releaseDate && (
          <DateFormatter date={lastUpgrade.releaseDate} long>
            {(formattedDate) => (
              <span>{translateWithParameters('system.released_x', formattedDate)}</span>
            )}
          </DateFormatter>
        )}
        {lastUpgrade.changeLogUrl && (
          <Link className="spacer-left" to={lastUpgrade.changeLogUrl} target="_blank">
            {translate('system.release_notes')}
          </Link>
        )}
      </div>
      <SystemUpgradeIntermediate className="spacer-top" upgrades={systemUpgrades.slice(1)} />
      <div className="big-spacer-top">
        <a
          className="button"
          download={getEditionDownloadFilename(downloadUrl)}
          href={downloadUrl}
          rel="noopener noreferrer"
          target="_blank"
        >
          {translateWithParameters('system.download_x', lastUpgrade.version)}
        </a>
        <DocLink className="spacer-left" to="/setup-and-upgrade/upgrade-the-server/upgrade-guide/">
          {translate('system.how_to_upgrade')}
        </DocLink>
      </div>
    </div>
  );
}
