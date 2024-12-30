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

import { isEmpty } from 'lodash';
import { Banner } from '~design-system';
import { getSystemUpgrades } from '../../../api/system';
import { DismissableAlert } from '../../../components/ui/DismissableAlert';
import { SystemUpgradeButton } from '../../../components/upgrade/SystemUpgradeButton';
import { UpdateUseCase } from '../../../components/upgrade/utils';
import { translate } from '../../../helpers/l10n';
import { isCurrentVersionEOLActive } from '../../../helpers/system';
import { ProductNameForUpgrade } from '../../../types/system';
import { useAppState } from '../app-state/withAppStateContext';
import { analyzeUpgrades, BANNER_VARIANT, isCurrentVersionLTA, parseVersion } from './helpers';

interface Props {
  data?: Awaited<ReturnType<typeof getSystemUpgrades>>;
  dismissable?: boolean;
}

export function SQSUpdateBanner({ data, dismissable }: Readonly<Props>) {
  const appState = useAppState();

  // below: undefined already tested upstream in UpdateNotification, ?? [] is just to make TS happy
  const parsedVersion = parseVersion(appState.version) ?? [];
  const { upgrades = [], installedVersionActive, latestLTA } = data ?? {};

  const SQSUpgrades = upgrades.filter(
    (upgrade) => upgrade.product === ProductNameForUpgrade.SonarQubeServer,
  );

  const active = installedVersionActive ?? isCurrentVersionEOLActive(appState.versionEOL);

  if (active && isEmpty(SQSUpgrades)) {
    return null;
  }

  const { isMinorUpdate, isPatchUpdate, latest } = analyzeUpgrades({
    parsedVersion,
    upgrades: SQSUpgrades,
  });

  let useCase = UpdateUseCase.NewVersion;

  if (!active) {
    useCase = UpdateUseCase.CurrentVersionInactive;
  } else if (
    isPatchUpdate &&
    // if the latest update is a patch and either we're running latest LTA, or there's no minor update
    ((latestLTA !== undefined && isCurrentVersionLTA(parsedVersion, latestLTA)) || !isMinorUpdate)
  ) {
    useCase = UpdateUseCase.NewPatch;
  }

  const dismissKey = useCase + (latest?.version ?? appState.version);

  const contents = (
    <>
      {translate('admin_notification.update', useCase)}

      <SystemUpgradeButton
        systemUpgrades={SQSUpgrades}
        updateUseCase={useCase}
        latestLTA={latestLTA}
      />
    </>
  );

  return dismissable ? (
    <DismissableAlert
      alertKey={dismissKey}
      variant={BANNER_VARIANT[useCase]}
      className={`it__promote-update-notification it__upgrade-prompt-${useCase}`}
    >
      {contents}
    </DismissableAlert>
  ) : (
    <Banner variant={BANNER_VARIANT[useCase]} className={`it__upgrade-prompt-${useCase}`}>
      {contents}
    </Banner>
  );
}
