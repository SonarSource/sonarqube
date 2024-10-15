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
import styled from '@emotion/styled';
import { Heading, Spinner } from '@sonarsource/echoes-react';
import {
  ButtonPrimary,
  ContentCell,
  NumericalCell,
  Table,
  TableRow,
  themeColor,
} from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { StaleTime } from '../../../queries/common';
import { useGetQualityProfile } from '../../../queries/quality-profiles';
import { useSearchRulesQuery } from '../../../queries/rules';
import { useStandardExperienceMode } from '../../../queries/settings';
import { CleanCodeAttributeCategory, SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { SearchRulesResponse } from '../../../types/coding-rules';
import { RulesFacetName } from '../../../types/rules';
import { RuleTypes } from '../../../types/types';
import { Profile } from '../types';
import ProfileRulesDeprecatedWarning from './ProfileRulesDeprecatedWarning';
import ProfileRulesRow from './ProfileRulesRow';
import ProfileRulesSonarWayComparison from './ProfileRulesSonarWayComparison';

interface Props {
  profile: Profile;
}

interface ByType {
  count: number | null;
  val: string;
}

export default function ProfileRules({ profile }: Readonly<Props>) {
  const { data: isStandardMode } = useStandardExperienceMode();
  const activateMoreUrl = getRulesUrl({ qprofile: profile.key, activation: 'false' });
  const { actions = {} } = profile;

  const { data: allRules, isLoading: isAllRulesLoading } = useSearchRulesQuery(
    {
      ps: 1,
      languages: profile.language,
      facets: isStandardMode
        ? `${RulesFacetName.Types}`
        : `${RulesFacetName.CleanCodeAttributeCategories},${RulesFacetName.ImpactSoftwareQualities}`,
    },
    { staleTime: StaleTime.LIVE },
  );

  const { data: activatedRules, isLoading: isActivatedRulesLoading } = useSearchRulesQuery(
    {
      ps: 1,
      activation: 'true',
      facets: isStandardMode
        ? `${RulesFacetName.Types}`
        : `${RulesFacetName.CleanCodeAttributeCategories},${RulesFacetName.ImpactSoftwareQualities}`,
      qprofile: profile.key,
    },
    { enabled: !!allRules, staleTime: StaleTime.LIVE },
  );

  const { data: sonarWayDiff, isLoading: isShowProfileLoading } = useGetQualityProfile(
    { compareToSonarWay: true, profile },
    { enabled: !profile.isBuiltIn, select: (data) => data.compareToSonarWay },
  );

  const findFacet = React.useCallback((response: SearchRulesResponse, property: string) => {
    const facet = response.facets?.find((f) => f.property === property);
    return facet ? facet.values : [];
  }, []);

  const extractFacetData = React.useCallback(
    (facetName: string, response: SearchRulesResponse | null | undefined) => {
      if (!response) {
        return {};
      }

      return keyBy<ByType>(findFacet(response, facetName), 'val');
    },
    [findFacet],
  );

  const totalByCctCategory = extractFacetData(
    RulesFacetName.CleanCodeAttributeCategories,
    allRules,
  );
  const countsByCctCategory = extractFacetData(
    RulesFacetName.CleanCodeAttributeCategories,
    activatedRules,
  );
  const totalBySoftwareQuality = extractFacetData(RulesFacetName.ImpactSoftwareQualities, allRules);
  const countsBySoftwareImpact = extractFacetData(
    RulesFacetName.ImpactSoftwareQualities,
    activatedRules,
  );
  const totalByTypes = extractFacetData(RulesFacetName.Types, allRules);
  const countsByTypes = extractFacetData(RulesFacetName.Types, activatedRules);

  return (
    <section aria-label={translate('rules')} className="it__quality-profiles__rules">
      <Spinner isLoading={isActivatedRulesLoading || isAllRulesLoading || isShowProfileLoading}>
        <Heading className="sw-mb-4" as="h2">
          {translate('quality_profile.rules.breakdown')}
        </Heading>

        {isStandardMode && (
          <Table
            columnCount={3}
            columnWidths={['50%', '25%', '25%']}
            header={
              <StyledTableRowHeader>
                <ContentCell className="sw-font-semibold sw-pl-4">{translate('type')}</ContentCell>
                <NumericalCell className="sw-font-regular">{translate('active')}</NumericalCell>
                <NumericalCell className="sw-pr-4 sw-font-regular">
                  {translate('inactive')}
                </NumericalCell>
              </StyledTableRowHeader>
            }
            noHeaderTopBorder
            noSidePadding
            withRoundedBorder
          >
            {RuleTypes.filter((type) => type !== 'UNKNOWN').map((type) => (
              <ProfileRulesRow
                title={translate('issue.type', type, 'plural')}
                total={totalByTypes[type]?.count}
                count={countsByTypes[type]?.count}
                key={type}
                qprofile={profile.key}
                type={type}
              />
            ))}
          </Table>
        )}

        {!isStandardMode && (
          <>
            <Table
              className="sw-mb-4"
              columnCount={3}
              columnWidths={['50%', '25%', '25%']}
              header={
                <StyledTableRowHeader>
                  <ContentCell className="sw-font-semibold sw-pl-4">
                    {translate('quality_profile.rules.software_qualities_title')}
                  </ContentCell>
                  <NumericalCell className="sw-font-regular">{translate('active')}</NumericalCell>
                  <NumericalCell className="sw-pr-4 sw-font-regular">
                    {translate('inactive')}
                  </NumericalCell>
                </StyledTableRowHeader>
              }
              noHeaderTopBorder
              noSidePadding
              withRoundedBorder
            >
              {Object.values(SoftwareQuality).map((quality) => (
                <ProfileRulesRow
                  title={translate('software_quality', quality)}
                  total={totalBySoftwareQuality[quality]?.count}
                  count={countsBySoftwareImpact[quality]?.count}
                  key={quality}
                  qprofile={profile.key}
                  propertyName={RulesFacetName.ImpactSoftwareQualities}
                  propertyValue={quality}
                />
              ))}
            </Table>

            <Table
              columnCount={3}
              columnWidths={['50%', '25%', '25%']}
              header={
                <StyledTableRowHeader>
                  <ContentCell className="sw-font-semibold sw-pl-4">
                    {translate('quality_profile.rules.cct_categories_title')}
                  </ContentCell>
                  <NumericalCell className="sw-font-regular">{translate('active')}</NumericalCell>
                  <NumericalCell className="sw-pr-4 sw-font-regular">
                    {translate('inactive')}
                  </NumericalCell>
                </StyledTableRowHeader>
              }
              noHeaderTopBorder
              noSidePadding
              withRoundedBorder
            >
              {Object.values(CleanCodeAttributeCategory).map((category) => (
                <ProfileRulesRow
                  title={translate('rule.clean_code_attribute_category', category)}
                  total={totalByCctCategory[category]?.count}
                  count={countsByCctCategory[category]?.count}
                  key={category}
                  qprofile={profile.key}
                  propertyName={RulesFacetName.CleanCodeAttributeCategories}
                  propertyValue={category}
                />
              ))}
            </Table>
          </>
        )}

        <div className="sw-mt-6 sw-flex sw-flex-col sw-gap-4 sw-items-start">
          {profile.activeDeprecatedRuleCount > 0 && (
            <ProfileRulesDeprecatedWarning
              activeDeprecatedRules={profile.activeDeprecatedRuleCount}
              profile={profile.key}
            />
          )}

          {isDefined(sonarWayDiff) && sonarWayDiff.missingRuleCount > 0 && (
            <ProfileRulesSonarWayComparison
              language={profile.language}
              profile={profile.key}
              sonarWayMissingRules={sonarWayDiff.missingRuleCount}
              sonarway={sonarWayDiff.profile}
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
            <DocHelpTooltip content={translate('quality_profiles.activate_more.help.built_in')}>
              <ButtonPrimary className="it__quality-profiles__activate-rules" disabled>
                {translate('quality_profiles.activate_more')}
              </ButtonPrimary>
            </DocHelpTooltip>
          )}
        </div>
      </Spinner>
    </section>
  );
}

const StyledTableRowHeader = styled(TableRow)`
  background-color: ${themeColor('breakdownHeaderBackground')};
`;
