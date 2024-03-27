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
import { Banner, Variant } from 'design-system';
import { groupBy, isEmpty, mapValues } from 'lodash';
import * as React from 'react';
import DismissableAlert from '../../../components/ui/DismissableAlert';
import SystemUpgradeButton from '../../../components/upgrade/SystemUpgradeButton';
import { UpdateUseCase } from '../../../components/upgrade/utils';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { useSystemUpgrades } from '../../../queries/system';
import { AppState } from '../../../types/appstate';
import { Permissions } from '../../../types/permissions';
import { Dict } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import withAppStateContext from '../app-state/withAppStateContext';
import withCurrentUserContext from '../current-user/withCurrentUserContext';
import { isMinorUpdate, isPatchUpdate, isPreLTSUpdate, isPreviousLTSUpdate } from './helpers';

const MAP_VARIANT: Dict<Variant> = {
  [UpdateUseCase.NewMinorVersion]: 'info',
  [UpdateUseCase.NewPatch]: 'warning',
  [UpdateUseCase.PreLTS]: 'warning',
  [UpdateUseCase.PreviousLTS]: 'error',
};

interface Props {
  dismissable: boolean;
  appState: AppState;
  currentUser: CurrentUser;
}

const VERSION_PARSER = /^(\d+)\.(\d+)(\.(\d+))?/;

export function UpdateNotification({ dismissable, appState, currentUser }: Readonly<Props>) {
  const canUserSeeNotification =
    isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.Admin);
  const regExpParsedVersion = VERSION_PARSER.exec(appState.version);
  const { data } = useSystemUpgrades({
    enabled: canUserSeeNotification && regExpParsedVersion !== null,
  });

  if (
    !canUserSeeNotification ||
    regExpParsedVersion === null ||
    data === undefined ||
    isEmpty(data.upgrades)
  ) {
    return null;
  }

  const { upgrades, latestLTS } = data;
  const parsedVersion = regExpParsedVersion
    .slice(1)
    .map(Number)
    .map((n) => (isNaN(n) ? 0 : n));

  const systemUpgrades = mapValues(
    groupBy(upgrades, (upgrade) => {
      const [major] = upgrade.version.split('.');
      return major;
    }),
    (upgrades) =>
      groupBy(upgrades, (upgrade) => {
        const [, minor] = upgrade.version.split('.');
        return minor;
      }),
  );

  let useCase = UpdateUseCase.NewMinorVersion;

  if (isPreviousLTSUpdate(parsedVersion, latestLTS, systemUpgrades)) {
    useCase = UpdateUseCase.PreviousLTS;
  } else if (isPreLTSUpdate(parsedVersion, latestLTS)) {
    useCase = UpdateUseCase.PreLTS;
  } else if (isPatchUpdate(parsedVersion, systemUpgrades)) {
    useCase = UpdateUseCase.NewPatch;
  } else if (isMinorUpdate(parsedVersion, systemUpgrades)) {
    useCase = UpdateUseCase.NewMinorVersion;
  }

  const latest = [...upgrades].sort(
    (upgrade1, upgrade2) =>
      new Date(upgrade2.releaseDate ?? '').getTime() -
      new Date(upgrade1.releaseDate ?? '').getTime(),
  )[0];

  const dismissKey = useCase + latest.version;

  return dismissable ? (
    <DismissableAlert
      alertKey={dismissKey}
      variant={MAP_VARIANT[useCase]}
      className={`it__promote-update-notification it__upgrade-prompt-${useCase}`}
    >
      {translate('admin_notification.update', useCase)}
      <SystemUpgradeButton
        systemUpgrades={upgrades}
        updateUseCase={useCase}
        latestLTS={latestLTS}
      />
    </DismissableAlert>
  ) : (
    <Banner variant={MAP_VARIANT[useCase]} className={`it__upgrade-prompt-${useCase}`}>
      {translate('admin_notification.update', useCase)}
      <SystemUpgradeButton
        systemUpgrades={upgrades}
        updateUseCase={useCase}
        latestLTS={latestLTS}
      />
    </Banner>
  );
}

export default withCurrentUserContext(withAppStateContext(UpdateNotification));
