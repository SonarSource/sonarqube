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

import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useAppState } from '../../app/components/app-state/withAppStateContext';
import { useDocUrl } from '../../helpers/docs';
import { getInstanceVersionNumber } from '../../helpers/strings';
import { useSystemUpgrades } from '../../queries/system';

export default function AppVersionStatus() {
  const { data } = useSystemUpgrades();
  const { version } = useAppState();

  const docUrl = useDocUrl();
  const intl = useIntl();

  return intl.formatMessage(
    { id: `footer.version` },
    {
      version: getInstanceVersionNumber(version),
      status:
        data?.installedVersionActive !== undefined ? (
          <LinkStandalone
            className="sw-ml-1"
            highlight={LinkHighlight.CurrentColor}
            to={docUrl('/setup-and-upgrade/upgrade-the-server/active-versions/')}
          >
            <FormattedMessage
              id={`footer.version.status.${data.installedVersionActive ? 'active' : 'inactive'}`}
            />
          </LinkStandalone>
        ) : (
          ''
        ),
    },
  );
}
