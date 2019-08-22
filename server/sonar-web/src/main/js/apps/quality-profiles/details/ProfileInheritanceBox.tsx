/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileLink from '../components/ProfileLink';

interface Props {
  className?: string;
  depth: number;
  displayLink?: boolean;
  extendsBuiltIn?: boolean;
  language: string;
  organization: string | null;
  profile: T.ProfileInheritanceDetails;
  type?: string;
}

export default function ProfileInheritanceBox(props: Props) {
  const {
    className,
    depth,
    extendsBuiltIn,
    language,
    organization,
    profile,
    displayLink = true,
    type = 'current'
  } = props;
  const offset = 25 * depth;

  return (
    <tr className={className} data-test={`quality-profiles__inheritance-${type}`}>
      <td>
        <div style={{ paddingLeft: offset }}>
          {displayLink ? (
            <ProfileLink
              className="text-middle"
              language={language}
              name={profile.name}
              organization={organization}>
              {profile.name}
            </ProfileLink>
          ) : (
            <span className="text-middle">{profile.name}</span>
          )}
          {profile.isBuiltIn && <BuiltInQualityProfileBadge className="spacer-left" />}
          {extendsBuiltIn && (
            <HelpTooltip
              className="spacer-left"
              overlay={translate('quality_profiles.extends_built_in')}
            />
          )}
        </div>
      </td>

      <td>{translateWithParameters('quality_profile.x_active_rules', profile.activeRuleCount)}</td>

      <td>
        {profile.overridingRuleCount != null && (
          <p>
            {translateWithParameters(
              'quality_profiles.x_overridden_rules',
              profile.overridingRuleCount
            )}
          </p>
        )}
      </td>
    </tr>
  );
}
