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
import { Link } from '@sonarsource/echoes-react';
import { FlagMessage } from 'design-system/lib';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { AlmKeys } from '../../../../types/alm-settings';

export default function MonorepoNoOrganisations({
  almKey,
  canAdmin,
}: Readonly<{ almKey: AlmKeys; canAdmin: boolean }>) {
  const { formatMessage } = useIntl();

  return (
    <FlagMessage variant="warning">
      <span>
        {canAdmin ? (
          <FormattedMessage
            id="onboarding.create_project.monorepo.warning.message_admin"
            defaultMessage={formatMessage({
              id: 'onboarding.create_project.monorepo.warning.message_admin',
            })}
            values={{
              almKey: formatMessage({ id: `alm.${almKey}` }),
              link: (
                <Link to="/admin/settings?category=almintegration">
                  <FormattedMessage id="onboarding.create_project.monorepo.warning.message_admin.link" />
                </Link>
              ),
            }}
          />
        ) : (
          <FormattedMessage
            id="onboarding.create_project.monorepo.warning.message"
            values={{ almKey: formatMessage({ id: `alm.${almKey}` }) }}
          />
        )}
      </span>
    </FlagMessage>
  );
}
