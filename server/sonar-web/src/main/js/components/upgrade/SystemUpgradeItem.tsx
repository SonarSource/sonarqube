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

import { Button, ButtonVariety, LinkStandalone } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { DownloadButton, SubHeading } from '~design-system';
import { useAppState } from '../../app/components/app-state/withAppStateContext';
import { DocLink } from '../../helpers/doc-links';
import {
  getEdition,
  getEditionDownloadFilename,
  getEditionDownloadUrl,
} from '../../helpers/editions';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { ProductName, SystemUpgrade } from '../../types/system';
import DocumentationLink from '../common/DocumentationLink';
import DateFormatter from '../intl/DateFormatter';
import SystemUpgradeIntermediate from './SystemUpgradeIntermediate';

export interface SystemUpgradeItemProps {
  edition: EditionKey | undefined;
  isLTAVersion: boolean;
  isPatch: boolean;
  systemUpgrades: SystemUpgrade[];
}

export function SystemUpgradeItem(props: Readonly<SystemUpgradeItemProps>) {
  const appState = useAppState();

  const { edition, isPatch, isLTAVersion, systemUpgrades } = props;

  const isCommunityBuildRunning = appState.edition === EditionKey.community;

  const lastUpgrade = systemUpgrades[0];

  const downloadUrl =
    getEditionDownloadUrl(getEdition(edition ?? EditionKey.community), lastUpgrade) ?? '';

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
          <LinkStandalone
            className="sw-ml-2"
            to="https://www.sonarsource.com/products/sonarqube/whats-new/?referrer=sonarqube"
          >
            {translate('system.see_whats_new')}
          </LinkStandalone>
        )}
      </SubHeading>

      <p>
        <FormattedMessage
          id="system.version_is_availble"
          values={{
            version: (
              <b>
                {`${ProductName.SonarQubeServer} ${
                  isCommunityBuildRunning
                    ? lastUpgrade.version.split('.').join(' Release ')
                    : lastUpgrade.version
                }`}
              </b>
            ),
          }}
        />
      </p>

      <p className="sw-mt-2">{lastUpgrade.description}</p>

      <div className="sw-mt-4">
        {lastUpgrade.releaseDate !== undefined && (
          <DateFormatter date={lastUpgrade.releaseDate} long>
            {(formattedDate) => (
              <span>{translateWithParameters('system.released_x', formattedDate)}</span>
            )}
          </DateFormatter>
        )}

        {lastUpgrade.changeLogUrl !== undefined && (
          <LinkStandalone className="sw-ml-2" to={lastUpgrade.changeLogUrl}>
            {translate('system.release_notes')}
          </LinkStandalone>
        )}
      </div>

      <SystemUpgradeIntermediate className="sw-mt-2" upgrades={systemUpgrades.slice(1)} />

      <div className="sw-mt-4">
        {isCommunityBuildRunning ? (
          <Button
            // WARNING! A button acting as a link is bad a11y. We should replace this with a
            // Call To Action (CTA) component from Echoes once it becomes available.
            onClick={() => {
              window.location.href = 'https://www.sonarsource.com/plans-and-pricing/sonarqube/';
            }}
            //
            variety={ButtonVariety.Primary}
          >
            {translate('learn_more')}
          </Button>
        ) : (
          <>
            <DownloadButton download={getEditionDownloadFilename(downloadUrl)} href={downloadUrl}>
              {translateWithParameters('system.download_x', lastUpgrade.version)}
            </DownloadButton>

            <DocumentationLink className="sw-ml-4" to={DocLink.ServerUpgradeRoadmap}>
              {translate('system.how_to_upgrade')}
            </DocumentationLink>
          </>
        )}
      </div>
    </div>
  );
}
