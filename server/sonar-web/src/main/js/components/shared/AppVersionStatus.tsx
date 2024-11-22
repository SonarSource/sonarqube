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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useAppState } from '../../app/components/app-state/withAppStateContext';
import { now, parseDate } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';
import DocLink from '../common/DocLink';

export default function AppVersionStatus() {
  const { version, versionEOL } = useAppState();
  const isActive = parseDate(versionEOL) > now();

  return (
    <FormattedMessage
      id="footer.version"
      defaultMessage={translate('footer.version')}
      values={{
        version,
        status: !isActive && (
          <DocLink
            to="/setup-and-upgrade/upgrade-the-server/active-versions/"
            className="little-spacer-left"
          >
            {translate('footer.version.status.inactive')}
          </DocLink>
        ),
      }}
    />
  );
}
