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

import { LinkStandalone } from '@sonarsource/echoes-react';
import { isEmpty } from 'lodash';
import { FormattedMessage } from 'react-intl';
import { Banner } from '~design-system';
import { getSystemUpgrades } from '../../../api/system';
import { DismissableAlert } from '../../../components/ui/DismissableAlert';
import { SystemUpgradeButton } from '../../../components/upgrade/SystemUpgradeButton';
import { UpdateUseCase } from '../../../components/upgrade/utils';
import { translate } from '../../../helpers/l10n';
import { ProductNameForUpgrade } from '../../../types/system';
import { useAppState } from '../app-state/withAppStateContext';
import { analyzeUpgrades, isVersionAPatchUpdate, parseVersion } from './helpers';

interface Props {
  data?: Awaited<ReturnType<typeof getSystemUpgrades>>;
  dismissable?: boolean;
}

export function SQCBUpdateBanners({ data, dismissable }: Readonly<Props>) {
  const appState = useAppState();

  const parsedVersion = parseVersion(appState.version);
  const { upgrades = [], latestLTA } = data ?? {};

  const SQSUpgrades = upgrades.filter(
    (upgrade) =>
      upgrade.product === ProductNameForUpgrade.SonarQubeServer &&
      !isVersionAPatchUpdate(upgrade.version),
  );

  const SQCBUpgrades = upgrades.filter(
    (upgrade) => upgrade.product === ProductNameForUpgrade.SonarQubeCommunityBuild,
  );

  const banners = [];

  if (!isEmpty(SQCBUpgrades)) {
    const contents = (
      <FormattedMessage
        id="admin_notification.update.new_sqcb_version"
        values={{
          link: (
            <LinkStandalone
              className="sw-ml-1"
              to="https://www.sonarsource.com/open-source-editions/sonarqube-community-edition/"
            >
              {translate('admin_notification.update.latest')}
            </LinkStandalone>
          ),
        }}
      />
    );

    const { latest } = analyzeUpgrades({
      parsedVersion,
      upgrades: SQCBUpgrades,
    });

    const dismissKey = latest?.version ?? appState.version;

    banners.push(
      dismissable ? (
        <DismissableAlert alertKey={dismissKey} key="SQCB" variant="info">
          {contents}
        </DismissableAlert>
      ) : (
        <Banner key="SQCB" variant="info">
          {contents}
        </Banner>
      ),
    );
  }

  if (!isEmpty(SQSUpgrades)) {
    const { latest } = analyzeUpgrades({
      parsedVersion,
      upgrades: SQSUpgrades,
    });

    const contents = (
      <>
        {translate('admin_notification.update.new_sqs_version_when_running_sqcb.banner')}{' '}
        {translate('admin_notification.update.new_sqs_version_when_running_sqcb.upgrade')}.
        <SystemUpgradeButton
          systemUpgrades={[latest]}
          updateUseCase={UpdateUseCase.NewVersion}
          latestLTA={latestLTA}
        />
      </>
    );

    const dismissKey = latest?.version ?? appState.version;

    banners.push(
      dismissable ? (
        <DismissableAlert alertKey={dismissKey} key="SQS" variant="info">
          {contents}
        </DismissableAlert>
      ) : (
        <Banner key="SQS" variant="info">
          {contents}
        </Banner>
      ),
    );
  }

  return banners;
}
