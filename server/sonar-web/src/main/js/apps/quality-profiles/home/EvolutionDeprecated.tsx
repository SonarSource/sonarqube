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
import { sortBy } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { DiscreetLink, FlagMessage, Note } from '~design-system';
import { isDefined } from '../../../helpers/types';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';
import { Profile } from '../types';
import { getProfilePath } from '../utils';

interface Props {
  organization: string;
  profiles: Profile[];
}

interface InheritedRulesInfo {
  count: number;
  from: Profile;
}

export default function EvolutionDeprecated({ organization, profiles }: Readonly<Props>) {
  const intl = useIntl();

  const profilesWithDeprecations = profiles.filter(
    (profile) => profile.activeDeprecatedRuleCount > 0,
  );

  if (profilesWithDeprecations.length === 0) {
    return null;
  }

  const sortedProfiles = sortBy(profilesWithDeprecations, (p) => -p.activeDeprecatedRuleCount);

  return (
    <section aria-label={intl.formatMessage({ id: 'quality_profiles.deprecated_rules' })}>
      <Heading as="h2" hasMarginBottom>
        {intl.formatMessage({ id: 'quality_profiles.deprecated_rules' })}
      </Heading>

      <FlagMessage variant="error" className="sw-mb-3">
        {intl.formatMessage(
          { id: 'quality_profiles.deprecated_rules_are_still_activated' },
          { count: profilesWithDeprecations.length },
        )}
      </FlagMessage>

      <ul className="sw-flex sw-flex-col sw-gap-4 sw-typo-default">
        {sortedProfiles.map((profile) => (
          <li className="sw-flex sw-flex-col sw-gap-1" key={profile.key}>
            <div className="sw-truncate">
              <DiscreetLink to={getProfilePath(profile.name, profile.language, organization)}>
                {profile.name}
              </DiscreetLink>
            </div>

            <Note>
              {profile.languageName}

              {', '}

              <DiscreetLink
                className="link-no-underline"
                to={getDeprecatedActiveRulesUrl({ qprofile: profile.key }, organization)}
                aria-label={intl.formatMessage(
                  { id: 'quality_profile.lang_deprecated_x_rules' },
                  { count: profile.activeDeprecatedRuleCount, name: profile.languageName },
                )}
              >
                {intl.formatMessage(
                  { id: 'quality_profile.x_rules' },
                  { count: profile.activeDeprecatedRuleCount },
                )}
              </DiscreetLink>
            </Note>

            <EvolutionDeprecatedInherited
              profile={profile}
              profilesWithDeprecations={profilesWithDeprecations}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}

function EvolutionDeprecatedInherited(
  props: Readonly<{
    profile: Profile;
    profilesWithDeprecations: Profile[];
  }>,
) {
  const { profile, profilesWithDeprecations } = props;
  const intl = useIntl();

  const rules = React.useMemo(
    () => getDeprecatedRulesInheritanceChain(profile, profilesWithDeprecations),
    [profile, profilesWithDeprecations],
  );

  if (rules.length === 0) {
    return null;
  }

  return (
    <>
      {rules.map((rule) => {
        if (rule.from.key === profile.key) {
          return null;
        }

        return (
          <Note key={rule.from.key}>
            {intl.formatMessage(
              { id: 'coding_rules.filters.inheritance.x_inherited_from_y' },
              { count: rule.count, name: rule.from.name },
            )}
          </Note>
        );
      })}
    </>
  );
}

function getDeprecatedRulesInheritanceChain(profile: Profile, profilesWithDeprecations: Profile[]) {
  let rules: InheritedRulesInfo[] = [];
  let count = profile.activeDeprecatedRuleCount;

  if (count === 0) {
    return rules;
  }

  if (isDefined(profile.parentKey) && profile.parentKey !== '') {
    const parentProfile = profilesWithDeprecations.find((p) => p.key === profile.parentKey);

    if (parentProfile) {
      const parentRules = getDeprecatedRulesInheritanceChain(
        parentProfile,
        profilesWithDeprecations,
      );

      if (parentRules.length !== 0) {
        count -= parentRules.reduce((n, rule) => n + rule.count, 0);
        rules = rules.concat(parentRules);
      }
    }
  }

  if (count > 0) {
    rules.push({
      count,
      from: profile,
    });
  }

  return rules;
}
