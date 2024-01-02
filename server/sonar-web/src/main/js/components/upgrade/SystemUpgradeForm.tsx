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
import { filter, flatMap, isEmpty, negate } from 'lodash';
import * as React from 'react';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../types/appstate';
import { SystemUpgrade } from '../../types/system';
import Link from '../common/Link';
import Modal from '../controls/Modal';
import { ResetButtonLink } from '../controls/buttons';
import { Alert, AlertVariant } from '../ui/Alert';
import SystemUpgradeItem from './SystemUpgradeItem';
import { SYSTEM_VERSION_REGEXP, UpdateUseCase } from './utils';

interface Props {
  appState: AppState;
  onClose: () => void;
  systemUpgrades: SystemUpgrade[][];
  latestLTS: string;
  updateUseCase?: UpdateUseCase;
}

const MAP_ALERT: { [key in UpdateUseCase]?: AlertVariant } = {
  [UpdateUseCase.NewPatch]: 'warning',
  [UpdateUseCase.PreLTS]: 'warning',
  [UpdateUseCase.PreviousLTS]: 'error',
};

export function SystemUpgradeForm(props: Readonly<Props>) {
  const { appState, latestLTS, onClose, updateUseCase, systemUpgrades } = props;

  let systemUpgradesWithPatch: SystemUpgrade[][] = [];
  const alertVariant = updateUseCase ? MAP_ALERT[updateUseCase] : undefined;
  const header = translate('system.system_upgrade');
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
    let untilLTS = false;

    for (const upgrades of systemUpgrades) {
      if (untilLTS === false) {
        systemUpgradesWithPatch.push(upgrades);
        untilLTS = upgrades.some((upgrade) => upgrade.version.startsWith(latestLTS));
      }
    }
  }

  return (
    <Modal contentLabel={header} onRequestClose={onClose}>
      <div className="modal-head">
        <h2>{header}</h2>
      </div>

      <div className="modal-body">
        {alertVariant && updateUseCase && (
          <Alert variant={alertVariant} className={`it__upgrade-alert-${updateUseCase}`}>
            {translate('admin_notification.update', updateUseCase)}
          </Alert>
        )}
        {systemUpgradesWithPatch.map((upgrades) => (
          <SystemUpgradeItem
            edition={appState.edition}
            key={upgrades[upgrades.length - 1].version}
            systemUpgrades={upgrades}
            isPatch={upgrades === patches}
            isLTSVersion={upgrades.some((upgrade) => upgrade.version.startsWith(latestLTS))}
          />
        ))}
      </div>
      <div className="modal-foot">
        <Link
          className="pull-left link-no-underline display-flex-center"
          to="https://www.sonarsource.com/products/sonarqube/downloads/?referrer=sonarqube"
          target="_blank"
        >
          {translate('system.see_sonarqube_downloads')}
        </Link>
        <ResetButtonLink onClick={onClose}>{translate('cancel')}</ResetButtonLink>
      </div>
    </Modal>
  );
}

export default withAppStateContext(SystemUpgradeForm);
