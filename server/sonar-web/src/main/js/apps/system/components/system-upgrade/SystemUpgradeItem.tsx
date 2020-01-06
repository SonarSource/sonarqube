/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateFormatter from '../../../../components/intl/DateFormatter';
import {
  getEdition,
  getEditionDownloadFilename,
  getEditionDownloadUrl
} from '../../../../helpers/editions';
import { EditionKey } from '../../../../types/editions';
import { SystemUpgrade } from '../../../../types/system';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';

interface Props {
  edition: EditionKey | undefined;
  type: string;
  systemUpgrades: SystemUpgrade[];
}

export default function SystemUpgradeItem(props: Props) {
  const { edition, type, systemUpgrades } = props;
  const lastUpgrade = systemUpgrades[0];
  const downloadUrl = getEditionDownloadUrl(
    getEdition(edition || EditionKey.community),
    lastUpgrade
  );

  return (
    <div className="system-upgrade-version">
      <h3 className="h1 spacer-bottom">
        <strong>{type}</strong>
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
          <DateFormatter date={lastUpgrade.releaseDate} long={true}>
            {formattedDate => (
              <span>{translateWithParameters('system.released_x', formattedDate)}</span>
            )}
          </DateFormatter>
        )}
        {lastUpgrade.changeLogUrl && (
          <a
            className="spacer-left"
            href={lastUpgrade.changeLogUrl}
            rel="noopener noreferrer"
            target="_blank">
            {translate('system.release_notes')}
          </a>
        )}
      </div>
      <SystemUpgradeIntermediate className="spacer-top" upgrades={systemUpgrades.slice(1)} />
      <div className="big-spacer-top">
        <a
          className="button"
          download={getEditionDownloadFilename(downloadUrl)}
          href={downloadUrl}
          rel="noopener noreferrer"
          target="_blank">
          {translateWithParameters('system.download_x', lastUpgrade.version)}
        </a>
        <a
          className="spacer-left"
          href="https://redirect.sonarsource.com/doc/upgrading.html"
          rel="noopener noreferrer"
          target="_blank">
          {translate('system.how_to_upgrade')}
        </a>
      </div>
    </div>
  );
}
