/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  ButtonPrimary,
  ContentCell,
  NumericalCell,
  SubTitle,
  Table,
  TableRow,
} from 'design-system/lib';
import { keyBy } from 'lodash';
import * as React from 'react';
import { getQualityProfile } from '../../../api/quality-profiles';
import { searchRules } from '../../../api/rules';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { SearchRulesResponse } from '../../../types/coding-rules';
import { Dict } from '../../../types/types';
import { Profile } from '../types';
import ProfileRulesDeprecatedWarning from './ProfileRulesDeprecatedWarning';
import ProfileRulesRow from './ProfileRulesRow';
import ProfileRulesSonarWayComparison from './ProfileRulesSonarWayComparison';

const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL', 'SECURITY_HOTSPOT'];

interface Props {
  profile: Profile;
}

interface ByType {
  val: string;
  count: number | null;
}

interface State {
  activatedTotal: number | null;
  activatedByType: Dict<ByType>;
  allByType: Dict<ByType>;
  compareToSonarWay: { profile: string; profileName: string; missingRuleCount: number } | null;
  total: number | null;
}

export default class ProfileRules extends React.PureComponent<Readonly<Props>, State> {
  mounted = false;

  state: State = {
    activatedTotal: null,
    activatedByType: keyBy(
      TYPES.map((t) => ({ val: t, count: null })),
      'val',
    ),
    allByType: keyBy(
      TYPES.map((t) => ({ val: t, count: null })),
      'val',
    ),
    compareToSonarWay: null,
    total: null,
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
      profile: this.props.profile,
    });
  }

  loadAllRules() {
    return searchRules({
      languages: this.props.profile.language,
      facets: 'types',
      ps: 1,
    });
  }

  loadActivatedRules() {
    return searchRules({
      activation: 'true',
      facets: 'types',
      ps: 1,
      qprofile: this.props.profile.key,
    });
  }

  loadRules() {
    return Promise.all([this.loadAllRules(), this.loadActivatedRules(), this.loadProfile()]).then(
      (responses) => {
        if (this.mounted) {
          const [allRules, activatedRules, showProfile] = responses;
          this.setState({
            activatedTotal: activatedRules.paging.total,
            allByType: keyBy<ByType>(this.takeFacet(allRules, 'types'), 'val'),
            activatedByType: keyBy<ByType>(this.takeFacet(activatedRules, 'types'), 'val'),
            compareToSonarWay: showProfile?.compareToSonarWay,
            total: allRules.paging.total,
          });
        }
      },
    );
  }

  takeFacet(response: SearchRulesResponse, property: string) {
    const facet = response.facets?.find((f) => f.property === property);
    return facet ? facet.values : [];
  }

  render() {
    const { profile } = this.props;
    const { compareToSonarWay } = this.state;
    const activateMoreUrl = getRulesUrl({ qprofile: profile.key, activation: 'false' });
    const { actions = {} } = profile;

    return (
      <section aria-label={translate('rules')} className="it__quality-profiles__rules">
        <Table
          columnCount={3}
          columnWidths={['50%', '25%', '25%']}
          header={
            <TableRow>
              <ContentCell>
                <SubTitle className="sw-mb-0">{translate('rules')}</SubTitle>
              </ContentCell>
              <NumericalCell>{translate('active')}</NumericalCell>
              <NumericalCell>{translate('inactive')}</NumericalCell>
            </TableRow>
          }
          noHeaderTopBorder
          noSidePadding
        >
          <ProfileRulesRow
            count={this.state.activatedTotal}
            qprofile={profile.key}
            total={this.state.total}
          />
          {TYPES.map((type) => (
            <ProfileRulesRow
              count={this.state.activatedByType[type]?.count}
              key={type}
              qprofile={profile.key}
              total={this.state.allByType[type]?.count}
              type={type}
            />
          ))}
        </Table>

        <div className="sw-mt-6 sw-flex sw-flex-col sw-gap-4 sw-items-start">
          {profile.activeDeprecatedRuleCount > 0 && (
            <ProfileRulesDeprecatedWarning
              activeDeprecatedRules={profile.activeDeprecatedRuleCount}
              profile={profile.key}
            />
          )}

          {isDefined(compareToSonarWay) && compareToSonarWay.missingRuleCount > 0 && (
            <ProfileRulesSonarWayComparison
              language={profile.language}
              profile={profile.key}
              sonarWayMissingRules={compareToSonarWay.missingRuleCount}
              sonarway={compareToSonarWay.profile}
            />
          )}

          {actions.edit && !profile.isBuiltIn && (
            <ButtonPrimary className="it__quality-profiles__activate-rules" to={activateMoreUrl}>
              {translate('quality_profiles.activate_more')}
            </ButtonPrimary>
          )}

          {/* if a user is allowed to `copy` a profile if they are a global admin */}
          {/* this user could potentially activate more rules if the profile was not built-in */}
          {/* in such cases it's better to show the button but disable it with a tooltip */}
          {actions.copy && profile.isBuiltIn && (
            <DocumentationTooltip
              content={translate('quality_profiles.activate_more.help.built_in')}
            >
              <ButtonPrimary className="it__quality-profiles__activate-rules" disabled>
                {translate('quality_profiles.activate_more')}
              </ButtonPrimary>
            </DocumentationTooltip>
          )}
        </div>
      </section>
    );
  }
}
