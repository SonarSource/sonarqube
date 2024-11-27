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

import { Button, ButtonVariety, LinkStandalone, Modal } from '@sonarsource/echoes-react';
import { filter, flatMap, isEmpty, negate } from 'lodash';
import { FlagMessage } from '~design-system';
import { useAppState } from '../../app/components/app-state/withAppStateContext';
import { BANNER_VARIANT } from '../../app/components/update-notification/helpers';
import { translate } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { SystemUpgrade } from '../../types/system';
import { SystemUpgradeItem } from './SystemUpgradeItem';
import { SYSTEM_VERSION_REGEXP, UpdateUseCase } from './utils';

interface Props {
  latestLTA?: string;
  onClose: () => void;
  systemUpgrades: SystemUpgrade[][];
  updateUseCase: UpdateUseCase;
}

export function SystemUpgradeForm(props: Readonly<Props>) {
  const appState = useAppState();

  const { latestLTA, onClose, updateUseCase, systemUpgrades } = props;

  const isCommunityBuildRunning = appState.edition === EditionKey.community;

  let systemUpgradesWithPatch: SystemUpgrade[][] = [];

  const alertVariant =
    updateUseCase !== UpdateUseCase.NewVersion ? BANNER_VARIANT[updateUseCase] : undefined;

  const parsedVersion = SYSTEM_VERSION_REGEXP.exec(appState.version);

  let patches: SystemUpgrade[] = [];

  if (updateUseCase === UpdateUseCase.NewPatch && parsedVersion !== null) {
    const [, major, minor] = parsedVersion;
    const majoMinorVersion = `${major}.${minor}`;

    patches = flatMap(systemUpgrades, (upgrades) =>
      filter(upgrades, (upgrade) => upgrade.version.startsWith(majoMinorVersion)),
    );

    systemUpgradesWithPatch = systemUpgrades
      .map((upgrades) =>
        upgrades.filter((upgrade) => !upgrade.version.startsWith(majoMinorVersion)),
      )
      .filter(negate(isEmpty));

    systemUpgradesWithPatch.push(patches);
  } else {
    let untilLTA = false;

    for (const upgrades of systemUpgrades) {
      if (untilLTA === false) {
        systemUpgradesWithPatch.push(upgrades);

        untilLTA = upgrades.some(
          (upgrade) => latestLTA !== undefined && upgrade.version.startsWith(latestLTA),
        );
      }
    }
  }

  return (
    <Modal
      content={
        <div className="sw-mt-4">
          {alertVariant && (
            <FlagMessage variant={alertVariant} className={`it__upgrade-alert-${updateUseCase}`}>
              {translate('admin_notification.update', updateUseCase)}
            </FlagMessage>
          )}

          {systemUpgradesWithPatch.map((upgrades) => (
            <SystemUpgradeItem
              edition={appState.edition}
              key={upgrades[upgrades.length - 1].version}
              systemUpgrades={upgrades}
              isPatch={upgrades === patches}
              isLTAVersion={upgrades.some(
                (upgrade) => latestLTA !== undefined && upgrade.version.startsWith(latestLTA),
              )}
            />
          ))}
        </div>
      }
      {...(isCommunityBuildRunning && {
        description: translate(
          'admin_notification.update.new_sqs_version_when_running_sqcb.upgrade',
        ),
      })}
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) {
          onClose();
        }
      }}
      primaryButton={
        !isCommunityBuildRunning && (
          <LinkStandalone
            className="sw-mr-8"
            to="https://www.sonarsource.com/products/sonarqube/downloads/?referrer=sonarqube"
          >
            {translate('system.see_sonarqube_downloads')}
          </LinkStandalone>
        )
      }
      secondaryButton={
        <Button onClick={onClose} variety={ButtonVariety.Default}>
          {translate('cancel')}
        </Button>
      }
      title={translate(
        isCommunityBuildRunning
          ? 'admin_notification.update.new_sqs_version_when_running_sqcb.modal'
          : 'system.system_upgrade',
      )}
    />
  );
}
