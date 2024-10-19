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

import { Heading } from '@sonarsource/echoes-react';
import { DiscreetLink, FlagMessage, Note } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import DateFormatter from '../../../components/intl/DateFormatter';
import { isDefined } from '../../../helpers/types';
import { Profile } from '../types';
import { getProfilePath, isStagnant } from '../utils';

interface Props {
  organization: string;
  profiles: Profile[];
}

export default function EvolutionStagnant(props: Readonly<Props>) {
  const intl = useIntl();
  const outdated = props.profiles.filter((profile) => !profile.isBuiltIn && isStagnant(profile));

  if (outdated.length === 0) {
    return null;
  }

  return (
    <section aria-label={intl.formatMessage({ id: 'quality_profiles.stagnant_profiles' })}>
      <Heading as="h2" hasMarginBottom>
        {intl.formatMessage({ id: 'quality_profiles.stagnant_profiles' })}
      </Heading>

      <FlagMessage variant="warning" className="sw-mb-3">
        {intl.formatMessage({ id: 'quality_profiles.not_updated_more_than_year' })}
      </FlagMessage>

      <ul className="sw-flex sw-flex-col sw-gap-4 sw-typo-default">
        {outdated.map((profile) => (
          <li className="sw-flex sw-flex-col sw-gap-1" key={profile.key}>
            <div className="sw-truncate">
              <DiscreetLink to={getProfilePath(profile.name, profile.language, props.organization)}>
                {profile.name}
              </DiscreetLink>
            </div>

            {isDefined(profile.rulesUpdatedAt) && profile.rulesUpdatedAt !== '' && (
              <Note>
                <DateFormatter date={profile.rulesUpdatedAt} long>
                  {(formattedDate) =>
                    intl.formatMessage(
                      { id: 'quality_profiles.x_updated_on_y' },
                      { name: profile.languageName, date: formattedDate },
                    )
                  }
                </DateFormatter>
              </Note>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
