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
import { useMemo } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useAppState } from '../../app/components/app-state/withAppStateContext';
import { DocLink } from '../../helpers/doc-links';
import { useDocUrl } from '../../helpers/docs';
import { getInstanceVersionNumber } from '../../helpers/strings';
import { isCurrentVersionEOLActive } from '../../helpers/system';
import { useSystemUpgrades } from '../../queries/system';
import { EditionKey } from '../../types/editions';

export default function AppVersionStatus({ statusOnly }: Readonly<{ statusOnly?: boolean }>) {
  const { data } = useSystemUpgrades();
  const { edition, version, versionEOL } = useAppState();

  const isActiveVersion = useMemo(() => {
    if (data?.installedVersionActive !== undefined) {
      return data.installedVersionActive;
    }

    return isCurrentVersionEOLActive(versionEOL);
  }, [data?.installedVersionActive, versionEOL]);

  const docUrl = useDocUrl(DocLink.ActiveVersions);
  const intl = useIntl();

  return intl.formatMessage(
    { id: statusOnly ? `footer.version.status` : `footer.version.full` },
    {
      version: getInstanceVersionNumber(version),
      status: edition && edition !== EditionKey.community && (
        <LinkStandalone className="sw-ml-1" highlight={LinkHighlight.CurrentColor} to={docUrl}>
          <FormattedMessage
            id={`footer.version.status.${isActiveVersion ? 'active' : 'inactive'}`}
          />
        </LinkStandalone>
      ),
    },
  );
}
