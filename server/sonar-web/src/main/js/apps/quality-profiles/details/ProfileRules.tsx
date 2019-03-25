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
import { Link } from 'react-router';
import { keyBy } from 'lodash';
import ProfileRulesRowOfType from './ProfileRulesRowOfType';
import ProfileRulesRowTotal from './ProfileRulesRowTotal';
import ProfileRulesDeprecatedWarning from './ProfileRulesDeprecatedWarning';
import ProfileRulesSonarWayComparison from './ProfileRulesSonarWayComparison';
import { searchRules, takeFacet } from '../../../api/rules';
import { getQualityProfile } from '../../../api/quality-profiles';
import { getRulesUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';
import { Button } from '../../../components/ui/buttons';
import DocTooltip from '../../../components/docs/DocTooltip';

const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL', 'SECURITY_HOTSPOT'];

interface Props {
  organization: string | null;
  profile: Profile;
}

interface ByType {
  val: string;
  count: number | null;
}

interface State {
  activatedTotal: number | null;
  activatedByType: T.Dict<ByType>;
  allByType: T.Dict<ByType>;
  compareToSonarWay: { profile: string; profileName: string; missingRuleCount: number } | null;
  total: number | null;
}

export default class ProfileRules extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    activatedTotal: null,
    activatedByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val'),
    allByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val'),
    compareToSonarWay: null,
    total: null
  };

  componentDidMount() {
    this.mounted = true;
    this.loadRules();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.profile.key !== this.props.profile.key) {
      this.loadRules();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadProfile() {
    if (this.props.profile.isBuiltIn) {
      return Promise.resolve(null);
    }
    return getQualityProfile({
      compareToSonarWay: true,
      profile: this.props.profile.key
    });
  }

  loadAllRules() {
    return searchRules({
      languages: this.props.profile.language,
      facets: 'types',
      organization: this.props.organization || undefined,
      ps: 1
    });
  }

  loadActivatedRules() {
    return searchRules({
      activation: 'true',
      facets: 'types',
      organization: this.props.organization || undefined,
      ps: 1,
      qprofile: this.props.profile.key
    });
  }

  loadRules() {
    Promise.all([this.loadAllRules(), this.loadActivatedRules(), this.loadProfile()]).then(
      responses => {
        if (this.mounted) {
          const [allRules, activatedRules, showProfile] = responses;
          this.setState({
            activatedTotal: activatedRules.total,
            allByType: keyBy<ByType>(takeFacet(allRules, 'types'), 'val'),
            activatedByType: keyBy<ByType>(takeFacet(activatedRules, 'types'), 'val'),
            compareToSonarWay: showProfile && showProfile.compareToSonarWay,
            total: allRules.total
          });
        }
      }
    );
  }

  getRulesCountForType(type: string) {
    return this.state.activatedByType && this.state.activatedByType[type]
      ? this.state.activatedByType[type].count
      : null;
  }

  getRulesTotalForType(type: string) {
    return this.state.allByType && this.state.allByType[type]
      ? this.state.allByType[type].count
      : null;
  }

  render() {
    const { organization, profile } = this.props;
    const { compareToSonarWay } = this.state;
    const activateMoreUrl = getRulesUrl(
      { qprofile: profile.key, activation: 'false' },
      organization
    );
    const { actions = {} } = profile;

    return (
      <div className="boxed-group quality-profile-rules">
        <div className="quality-profile-rules-distribution">
          <table className="data condensed">
            <thead>
              <tr>
                <th>
                  <h2>{translate('rules')}</h2>
                </th>
                <th>{translate('active')}</th>
                <th>{translate('inactive')}</th>
              </tr>
            </thead>
            <tbody>
              <ProfileRulesRowTotal
                count={this.state.activatedTotal}
                organization={organization}
                qprofile={profile.key}
                total={this.state.total}
              />
              {TYPES.map(type => (
                <ProfileRulesRowOfType
                  count={this.getRulesCountForType(type)}
                  key={type}
                  organization={organization}
                  qprofile={profile.key}
                  total={this.getRulesTotalForType(type)}
                  type={type}
                />
              ))}
            </tbody>
          </table>

          {actions.edit && !profile.isBuiltIn && (
            <div className="text-right big-spacer-top">
              <Link className="button js-activate-rules" to={activateMoreUrl}>
                {translate('quality_profiles.activate_more')}
              </Link>
            </div>
          )}

          {/* if a user is allowed to `copy` a profile if they are a global or organization admin */}
          {/* this user could potentially active more rules if the profile was not built-in */}
          {/* in such cases it's better to show the button but disable it with a tooltip */}
          {actions.copy && profile.isBuiltIn && (
            <div className="text-right big-spacer-top">
              <DocTooltip
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-profiles/activate-rules-in-built-in-profile.md')}>
                <Button className="disabled js-activate-rules">
                  {translate('quality_profiles.activate_more')}
                </Button>
              </DocTooltip>
            </div>
          )}
        </div>
        {profile.activeDeprecatedRuleCount > 0 && (
          <ProfileRulesDeprecatedWarning
            activeDeprecatedRules={profile.activeDeprecatedRuleCount}
            organization={organization}
            profile={profile.key}
          />
        )}
        {compareToSonarWay != null && compareToSonarWay.missingRuleCount > 0 && (
          <ProfileRulesSonarWayComparison
            language={profile.language}
            organization={organization}
            profile={profile.key}
            sonarWayMissingRules={compareToSonarWay.missingRuleCount}
            sonarway={compareToSonarWay.profile}
          />
        )}
      </div>
    );
  }
}
