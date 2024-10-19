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
import { Banner } from 'design-system';
import { groupBy, isEmpty, mapValues } from 'lodash';
import * as React from 'react';
import DismissableAlert from '../../../components/ui/DismissableAlert';
import SystemUpgradeButton from '../../../components/upgrade/SystemUpgradeButton';
import { UpdateUseCase } from '../../../components/upgrade/utils';
import { translate } from '../../../helpers/l10n';
import { isCurrentVersionEOLActive } from '../../../helpers/system';
import { hasGlobalPermission } from '../../../helpers/users';
import { useSystemUpgrades } from '../../../queries/system';
import { Permissions } from '../../../types/permissions';
import { isLoggedIn } from '../../../types/users';
import { useAppState } from '../app-state/withAppStateContext';
import { useCurrentUser } from '../current-user/CurrentUserContext';
import { BANNER_VARIANT, isCurrentVersionLTA, isMinorUpdate, isPatchUpdate } from './helpers';

interface Props {
  dismissable?: boolean;
}

const VERSION_PARSER = /^(\d+)\.(\d+)(\.(\d+))?/;

export default function UpdateNotification({ dismissable }: Readonly<Props>) {
  const appState = useAppState();
  const { currentUser } = useCurrentUser();

  const canUserSeeNotification =
    isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.Admin);
  const regExpParsedVersion = VERSION_PARSER.exec(appState.version);

  const { data, isLoading } = useSystemUpgrades({
    enabled: canUserSeeNotification && regExpParsedVersion !== null,
  });

  if (!canUserSeeNotification || regExpParsedVersion === null || isLoading) {
    return null;
  }

  const { upgrades = [], installedVersionActive, latestLTA } = data ?? {};

  let active = installedVersionActive;

  if (installedVersionActive === undefined) {
    active = isCurrentVersionEOLActive(appState.versionEOL);
  }

  if (active && isEmpty(upgrades)) {
    return null;
  }

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

  let useCase = UpdateUseCase.NewVersion;

  if (!active) {
    useCase = UpdateUseCase.CurrentVersionInactive;
  } else if (
    isPatchUpdate(parsedVersion, systemUpgrades) &&
    ((latestLTA !== undefined && isCurrentVersionLTA(parsedVersion, latestLTA)) ||
      !isMinorUpdate(parsedVersion, systemUpgrades))
  ) {
    useCase = UpdateUseCase.NewPatch;
  }

  const latest = [...upgrades].sort(
    (upgrade1, upgrade2) =>
      new Date(upgrade2.releaseDate ?? '').getTime() -
      new Date(upgrade1.releaseDate ?? '').getTime(),
  )[0];

  const dismissKey = useCase + (latest?.version ?? appState.version);

  return dismissable ? (
    <DismissableAlert
      alertKey={dismissKey}
      variant={BANNER_VARIANT[useCase]}
      className={`it__promote-update-notification it__upgrade-prompt-${useCase}`}
    >
      {translate('admin_notification.update', useCase)}
      <SystemUpgradeButton
        systemUpgrades={upgrades}
        updateUseCase={useCase}
        latestLTA={latestLTA}
      />
    </DismissableAlert>
  ) : (
    <Banner variant={BANNER_VARIANT[useCase]} className={`it__upgrade-prompt-${useCase}`}>
      {translate('admin_notification.update', useCase)}
      <SystemUpgradeButton
        systemUpgrades={upgrades}
        updateUseCase={useCase}
        latestLTA={latestLTA}
      />
    </Banner>
  );
}
