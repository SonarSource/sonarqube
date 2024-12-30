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
import { hasGlobalPermission } from '../../../helpers/users';
import { useSystemUpgrades } from '../../../queries/system';
import { EditionKey } from '../../../types/editions';
import { Permissions } from '../../../types/permissions';
import { isLoggedIn } from '../../../types/users';
import { useAppState } from '../app-state/withAppStateContext';
import { useCurrentUser } from '../current-user/CurrentUserContext';
import { parseVersion } from './helpers';
import { SQCBUpdateBanners } from './SQCBUpdateBanners';
import { SQSUpdateBanner } from './SQSUpdateBanner';

interface Props {
  dismissable?: boolean;
}

export function UpdateNotification({ dismissable }: Readonly<Props>) {
  const appState = useAppState();
  const { currentUser } = useCurrentUser();

  const canUserSeeNotification =
    isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.Admin);

  const parsedVersion = parseVersion(appState.version);

  const { data, isLoading } = useSystemUpgrades({
    enabled: canUserSeeNotification && parsedVersion !== undefined,
  });

  if (!canUserSeeNotification || parsedVersion === undefined || isLoading) {
    return null;
  }

  const isCommunityBuildRunning = appState.edition === EditionKey.community;

  if (isCommunityBuildRunning && !isEmpty(data?.upgrades)) {
    // We're running SQCB, show SQCB update banner & SQS update banner if applicable

    return <SQCBUpdateBanners data={data} dismissable={dismissable} />;
  }

  // We're running SQS (or old SQ), only show SQS update banner if applicable

  return <SQSUpdateBanner data={data} dismissable={dismissable} />;
}
