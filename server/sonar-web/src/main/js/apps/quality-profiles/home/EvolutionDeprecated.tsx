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
import { sortBy } from 'lodash';
import * as React from 'react';
import { Link } from 'react-router';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';
import ProfileLink from '../components/ProfileLink';
import { Profile } from '../types';

interface Props {
  organization: string | null;
  profiles: Profile[];
}

interface InheritedRulesInfo {
  count: number;
  from: Profile;
}

export default class EvolutionDeprecated extends React.PureComponent<Props> {
  getDeprecatedRulesInheritanceChain = (profile: Profile, profilesWithDeprecations: Profile[]) => {
    let rules: InheritedRulesInfo[] = [];
    let count = profile.activeDeprecatedRuleCount;

    if (count === 0) {
      return rules;
    }

    if (profile.parentKey) {
      const parentProfile = profilesWithDeprecations.find(p => p.key === profile.parentKey);
      if (parentProfile) {
        const parentRules = this.getDeprecatedRulesInheritanceChain(
          parentProfile,
          profilesWithDeprecations
        );
        if (parentRules.length) {
          count -= parentRules.reduce((n, rule) => n + rule.count, 0);
          rules = rules.concat(parentRules);
        }
      }
    }

    if (count > 0) {
      rules.push({
        count,
        from: profile
      });
    }

    return rules;
  };

  renderInheritedInfo = (profile: Profile, profilesWithDeprecations: Profile[]) => {
    const rules = this.getDeprecatedRulesInheritanceChain(profile, profilesWithDeprecations);
    if (rules.length) {
      return (
        <>
          {rules.map(rule => {
            if (rule.from.key === profile.key) {
              return null;
            }

            return (
              <div className="muted" key={rule.from.key}>
                {' '}
                {translateWithParameters(
                  'coding_rules.filters.inheritance.x_inherited_from_y',
                  rule.count,
                  rule.from.name
                )}
              </div>
            );
          })}
        </>
      );
    }
    return null;
  };

  render() {
    const profilesWithDeprecations = this.props.profiles.filter(
      profile => profile.activeDeprecatedRuleCount > 0
    );

    if (profilesWithDeprecations.length === 0) {
      return null;
    }

    const sortedProfiles = sortBy(profilesWithDeprecations, p => -p.activeDeprecatedRuleCount);

    return (
      <div className="boxed-group boxed-group-inner quality-profiles-evolution-deprecated">
        <div className="spacer-bottom">
          <strong>{translate('quality_profiles.deprecated_rules')}</strong>
        </div>
        <div className="spacer-bottom">
          {translateWithParameters(
            'quality_profiles.deprecated_rules_are_still_activated',
            profilesWithDeprecations.length
          )}
        </div>
        <ul>
          {sortedProfiles.map(profile => (
            <li className="spacer-top" key={profile.key}>
              <div className="text-ellipsis">
                <ProfileLink
                  className="link-no-underline"
                  language={profile.language}
                  name={profile.name}
                  organization={this.props.organization}>
                  {profile.name}
                </ProfileLink>
              </div>
              <div className="note">
                {profile.languageName}
                {', '}
                <Link
                  className="text-muted"
                  to={getDeprecatedActiveRulesUrl(
                    { qprofile: profile.key },
                    this.props.organization
                  )}>
                  {translateWithParameters(
                    'quality_profile.x_rules',
                    profile.activeDeprecatedRuleCount
                  )}
                </Link>
                {this.renderInheritedInfo(profile, profilesWithDeprecations)}
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  }
}
