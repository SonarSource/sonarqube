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
import {
  ActionCell,
  CellComponent,
  ContentCell,
  DiscreetLink,
  InheritanceIcon,
  Link,
  Note,
  SeparatorCircleIcon,
  SubTitle,
  Table,
  TableRow,
  TableRowInteractive,
  TextSubdued,
} from 'design-system';
import { filter } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Profile } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import {
  useActivateRuleMutation,
  useDeactivateRuleMutation,
} from '../../../queries/quality-profiles';
import { Dict, RuleActivation, RuleDetails } from '../../../types/types';
import BuiltInQualityProfileBadge from '../../quality-profiles/components/BuiltInQualityProfileBadge';
import ActivatedRuleActions from './ActivatedRuleActions';
import ActivationButton from './ActivationButton';

interface Props {
  activations: RuleActivation[] | undefined;
  canDeactivateInherited?: boolean;
  onActivate: () => void;
  onDeactivate: () => void;
  referencedProfiles: Dict<Profile>;
  ruleDetails: RuleDetails;
}

const MANDATORY_COLUMNS_COUNT = 2;

const PROFILES_HEADING_ID = 'rule-details-profiles-heading';

export default function RuleDetailsProfiles(props: Readonly<Props>) {
  const { activations = [], referencedProfiles, ruleDetails, canDeactivateInherited } = props;
  const { mutate: activateRule } = useActivateRuleMutation(props.onActivate);
  const { mutate: deactivateRule } = useDeactivateRuleMutation(props.onDeactivate);

  const canActivate = Object.values(referencedProfiles).some((profile) =>
    Boolean(profile.actions?.edit && profile.language === ruleDetails.lang),
  );
  const showParamsColumn =
    ruleDetails.templateKey === undefined &&
    ruleDetails?.params !== undefined &&
    ruleDetails.params.length > 0;

  const handleDeactivate = (key?: string) => {
    if (key !== undefined) {
      deactivateRule({
        key,
        rule: ruleDetails.key,
      });
    }
  };

  const handleRevert = (key?: string) => {
    if (key !== undefined) {
      activateRule({
        key,
        rule: ruleDetails.key,
        reset: true,
      });
    }
  };

  const renderRowActions = (activation: RuleActivation, profile: Profile) => {
    return (
      <ActionCell>
        <ActivatedRuleActions
          activation={activation}
          profile={profile}
          ruleDetails={ruleDetails}
          onActivate={props.onActivate}
          handleDeactivate={handleDeactivate}
          handleRevert={handleRevert}
          canDeactivateInherited={canDeactivateInherited}
        />
      </ActionCell>
    );
  };

  const renderActivationRow = (activation: RuleActivation) => {
    const profile = referencedProfiles[activation.qProfile];

    if (!profile) {
      return null;
    }

    const parentActivation = activations.find((x) => x.qProfile === profile.parentKey);

    const inheritedProfileSection = profile.parentName
      ? (activation.inherit === 'OVERRIDES' || activation.inherit === 'INHERITED') && (
          <Note as="div" className="sw-flex sw-items-center sw-w-full">
            <InheritanceIcon
              fill={activation.inherit === 'OVERRIDES' ? 'destructiveIconFocus' : 'currentColor'}
            />

            <DiscreetLink
              aria-label={`${translate('quality_profiles.parent')} ${profile.parentName}`}
              className="sw-ml-1 sw-truncate"
              title={profile.parentName}
              to={getQualityProfileUrl(profile.parentName, profile.language)}
            >
              {profile.parentName}
            </DiscreetLink>
          </Note>
        )
      : null;

    return (
      <TableRowInteractive key={profile.key}>
        <ContentCell className="sw-flex sw-flex-col sw-gap-2">
          <div className="sw-self-start sw-flex sw-gap-2 sw-items-center">
            <Link
              className="sw-truncate sw-max-w-64"
              aria-label={profile.name}
              title={profile.name}
              to={getQualityProfileUrl(profile.name, profile.language)}
            >
              {profile.name}
            </Link>

            {activation.prioritizedRule && (
              <>
                <SeparatorCircleIcon />
                <TextSubdued>{translate('coding_rules.prioritized_rule.title')}</TextSubdued>
              </>
            )}

            {profile.isBuiltIn && <BuiltInQualityProfileBadge />}
          </div>

          {inheritedProfileSection}
        </ContentCell>

        {showParamsColumn && (
          <CellComponent>
            {activation.params.map((param: { key: string; value: string }) => {
              const originalParam = parentActivation?.params.find((p) => p.key === param.key);
              const originalValue = originalParam?.value;

              return (
                <StyledParameter className="sw-my-4" key={param.key}>
                  <span className="key">{param.key}</span>

                  <span className="sep sw-mr-1">: </span>

                  <span className="value" title={param.value}>
                    {param.value}
                  </span>

                  {parentActivation && param.value !== originalValue && (
                    <div className="sw-flex sw-ml-4">
                      {translate('coding_rules.original')}

                      <span className="value sw-ml-1" title={originalValue}>
                        {originalValue}
                      </span>
                    </div>
                  )}
                </StyledParameter>
              );
            })}
          </CellComponent>
        )}

        {renderRowActions(activation, profile)}
      </TableRowInteractive>
    );
  };

  return (
    <div className="js-rule-profiles sw-mb-8">
      <div className="sw-flex sw-justify-between sw-items-end">
        <div>
          <SubTitle id={PROFILES_HEADING_ID}>
            <FormattedMessage id="coding_rules.quality_profiles" />
          </SubTitle>
          <Note>
            <FormattedMessage id="coding_rules.quality_profiles.description" />
          </Note>
        </div>

        {canActivate && (
          <ActivationButton
            buttonText={translate('coding_rules.activate')}
            modalHeader={translate('coding_rules.activate_in_quality_profile')}
            onDone={props.onActivate}
            profiles={filter(
              referencedProfiles,
              (profile) => !activations.find((activation) => activation.qProfile === profile.key),
            )}
            rule={ruleDetails}
          />
        )}
      </div>

      {activations.length > 0 && (
        <Table
          aria-labelledby={PROFILES_HEADING_ID}
          className="sw-my-6"
          columnCount={MANDATORY_COLUMNS_COUNT + +showParamsColumn}
          header={
            <TableRow>
              <ContentCell>{translate('profile_name')}</ContentCell>
              {showParamsColumn && <ContentCell>{translate('parameters')}</ContentCell>}
              <ActionCell>{translate('actions')}</ActionCell>
            </TableRow>
          }
          id="coding-rules-detail-quality-profiles"
        >
          {activations.map(renderActivationRow)}
        </Table>
      )}
    </div>
  );
}

const StyledParameter = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;

  .value {
    display: inline-block;
    max-width: 300px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;
