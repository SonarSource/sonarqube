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
import classNames from 'classnames';
import { ContentCell, DiscreetLink, TableRow } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { ProfileInheritanceDetails } from '../../../types/types';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileLink from '../components/ProfileLink';

const INDENT_PIXELS = 25;

interface Props {
  organization: string;
  className?: string;
  depth: number;
  displayLink?: boolean;
  language: string;
  profile: ProfileInheritanceDetails;
  type?: string;
}

export default function ProfileInheritanceRow(props: Readonly<Props>) {
  const { organization, className, depth, language, profile, displayLink = true, type = 'current' } = props;
  const activeRulesUrl = getRulesUrl({ qprofile: profile.key, activation: 'true' }, organization);
  const inactiveRulesUrl = getRulesUrl({ qprofile: profile.key, activation: 'false' }, organization);
  const overridingRulesUrl = getRulesUrl({
    activation: 'true',
    qprofile: profile.key,
    inheritance: 'OVERRIDES',
  }, organization);
  const offset = INDENT_PIXELS * depth;

  return (
    <TableRow className={classNames(`it__quality-profiles__inheritance-${type}`, className)}>
      <ContentCell>
        <div className="sw-flex sw-items-center sw-gap-2" style={{ paddingLeft: offset }}>
          {displayLink ? (
            <ProfileLink organization={organization} language={language} name={profile.name}>
              {profile.name}
            </ProfileLink>
          ) : (
            <span>{profile.name}</span>
          )}
          {profile.isBuiltIn && <BuiltInQualityProfileBadge />}
        </div>
      </ContentCell>

      <ContentCell>
        {profile.activeRuleCount > 0 ? (
          <DiscreetLink to={activeRulesUrl}>
            {translateWithParameters('quality_profile.x_active_rules', profile.activeRuleCount)}
          </DiscreetLink>
        ) : (
          translateWithParameters('quality_profile.x_active_rules', 0)
        )}
      </ContentCell>

      <ContentCell>
        {profile.inactiveRuleCount > 0 ? (
          <DiscreetLink to={inactiveRulesUrl}>
            {translateWithParameters(
              'quality_profile.x_inactive_rules',
              profile.inactiveRuleCount ?? 0,
            )}
          </DiscreetLink>
        ) : (
          translateWithParameters('quality_profile.x_inactive_rules', 0)
        )}
      </ContentCell>

      <ContentCell>
        {profile.overridingRuleCount != null && profile.overridingRuleCount > 0 ? (
          <DiscreetLink to={overridingRulesUrl}>
            {translateWithParameters(
              'quality_profiles.x_overridden_rules',
              profile.overridingRuleCount,
            )}
          </DiscreetLink>
        ) : (
          translateWithParameters('quality_profiles.x_overridden_rules', 0)
        )}
      </ContentCell>
    </TableRow>
  );
}
