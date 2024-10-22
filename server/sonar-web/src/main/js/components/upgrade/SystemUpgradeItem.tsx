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

import { FormattedMessage } from 'react-intl';
import { DownloadButton, Link, SubHeading } from '~design-system';
import { DocLink } from '../../helpers/doc-links';
import {
  getEdition,
  getEditionDownloadFilename,
  getEditionDownloadUrl,
} from '../../helpers/editions';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { SystemUpgrade } from '../../types/system';
import DocumentationLink from '../common/DocumentationLink';
import DateFormatter from '../intl/DateFormatter';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';

export interface SystemUpgradeItemProps {
  edition: EditionKey | undefined;
  isLTAVersion: boolean;
  isPatch: boolean;
  systemUpgrades: SystemUpgrade[];
}

export default function SystemUpgradeItem(props: SystemUpgradeItemProps) {
  const { edition, isPatch, isLTAVersion, systemUpgrades } = props;
  const lastUpgrade = systemUpgrades[0];
  const downloadUrl = getEditionDownloadUrl(
    getEdition(edition || EditionKey.community),
    lastUpgrade,
  );
  let header = translate('system.latest_version');
  if (isLTAVersion) {
    header = translate('system.lta_version');
  } else if (isPatch) {
    header = translate('system.latest_patch');
  }

  return (
    <div className="system-upgrade-version it__upgrade-list-item">
      <SubHeading as="h3">
        <strong>{header}</strong>
        {!isPatch && (
          <Link
            className="sw-ml-2"
            to="https://www.sonarsource.com/products/sonarqube/whats-new/?referrer=sonarqube"
          >
            {translate('system.see_whats_new')}
          </Link>
        )}
      </SubHeading>
      <p>
        <FormattedMessage
          defaultMessage={translate('system.version_is_availble')}
          id="system.version_is_availble"
          values={{ version: <b>SonarQube {lastUpgrade.version}</b> }}
        />
      </p>
      <p className="sw-mt-2">{lastUpgrade.description}</p>
      <div className="sw-mt-4">
        {lastUpgrade.releaseDate && (
          <DateFormatter date={lastUpgrade.releaseDate} long>
            {(formattedDate) => (
              <span>{translateWithParameters('system.released_x', formattedDate)}</span>
            )}
          </DateFormatter>
        )}
        {lastUpgrade.changeLogUrl && (
          <Link className="sw-ml-2" to={lastUpgrade.changeLogUrl}>
            {translate('system.release_notes')}
          </Link>
        )}
      </div>
      <SystemUpgradeIntermediate className="sw-mt-2" upgrades={systemUpgrades.slice(1)} />
      <div className="sw-mt-4">
        <DownloadButton download={getEditionDownloadFilename(downloadUrl)} href={downloadUrl}>
          {translateWithParameters('system.download_x', lastUpgrade.version)}
        </DownloadButton>

        <DocumentationLink className="sw-ml-2" to={DocLink.ServerUpgradeRoadmap}>
          {translate('system.how_to_upgrade')}
        </DocumentationLink>
      </div>
    </div>
  );
}
