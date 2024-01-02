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
  DangerButtonSecondary,
  DiscreetLink,
  InheritanceIcon,
  Link,
  Note,
  SubHeadingHighlight,
  Table,
  TableRowInteractive,
} from 'design-system';
import { filter } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Profile } from '../../../api/quality-profiles';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import {
  useActivateRuleMutation,
  useDeactivateRuleMutation,
} from '../../../queries/quality-profiles';
import { Dict, RuleActivation, RuleDetails } from '../../../types/types';
import BuiltInQualityProfileBadge from '../../quality-profiles/components/BuiltInQualityProfileBadge';
import ActivationButton from './ActivationButton';

interface Props {
  activations: RuleActivation[] | undefined;
  canDeactivateInherited?: boolean;
  onActivate: () => void;
  onDeactivate: () => void;
  referencedProfiles: Dict<Profile>;
  ruleDetails: RuleDetails;
}

const COLUMN_COUNT_WITH_PARAMS = 3;
const COLUMN_COUNT_WITHOUT_PARAMS = 2;

const PROFILES_HEADING_ID = 'rule-details-profiles-heading';

export default function RuleDetailsProfiles(props: Readonly<Props>) {
  const { activations = [], referencedProfiles, ruleDetails, canDeactivateInherited } = props;
  const { mutate: activateRule } = useActivateRuleMutation(props.onActivate);
  const { mutate: deactivateRule } = useDeactivateRuleMutation(props.onDeactivate);

  const canActivate = Object.values(referencedProfiles).some((profile) =>
    Boolean(profile.actions?.edit && profile.language === ruleDetails.lang),
  );

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
    const canEdit = profile.actions?.edit && !profile.isBuiltIn;
    const hasParent = activation.inherit !== 'NONE' && profile.parentKey;

    return (
      <ActionCell>
        {canEdit && (
          <>
            {!ruleDetails.isTemplate && (
              <ActivationButton
                activation={activation}
                ariaLabel={translateWithParameters('coding_rules.change_details_x', profile.name)}
                buttonText={translate('change_verb')}
                modalHeader={translate('coding_rules.change_details')}
                onDone={props.onActivate}
                profiles={[profile]}
                rule={ruleDetails}
              />
            )}

            {hasParent && activation.inherit === 'OVERRIDES' && profile.parentName && (
              <ConfirmButton
                confirmButtonText={translate('yes')}
                confirmData={profile.key}
                modalBody={translateWithParameters(
                  'coding_rules.revert_to_parent_definition.confirm',
                  profile.parentName,
                )}
                isDestructive
                modalHeader={translate('coding_rules.revert_to_parent_definition')}
                onConfirm={handleRevert}
              >
                {({ onClick }) => (
                  <DangerButtonSecondary className="sw-ml-2" onClick={onClick}>
                    {translate('coding_rules.revert_to_parent_definition')}
                  </DangerButtonSecondary>
                )}
              </ConfirmButton>
            )}

            {(!hasParent || canDeactivateInherited) && (
              <ConfirmButton
                confirmButtonText={translate('yes')}
                confirmData={profile.key}
                modalBody={translate('coding_rules.deactivate.confirm')}
                modalHeader={translate('coding_rules.deactivate')}
                onConfirm={handleDeactivate}
              >
                {({ onClick }) => (
                  <DangerButtonSecondary
                    className="sw-ml-2"
                    aria-label={translateWithParameters(
                      'coding_rules.deactivate_in_quality_profile_x',
                      profile.name,
                    )}
                    onClick={onClick}
                  >
                    {translate('coding_rules.deactivate')}
                  </DangerButtonSecondary>
                )}
              </ConfirmButton>
            )}
          </>
        )}
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
          <Note as="div" className="sw-flex sw-items-center sw-mt-2">
            <InheritanceIcon
              fill={activation.inherit === 'OVERRIDES' ? 'destructiveIconFocus' : 'currentColor'}
            />
            <DiscreetLink
              className="sw-ml-1"
              aria-label={`${translate('quality_profiles.parent')} ${profile.parentName}`}
              to={getQualityProfileUrl(profile.parentName, profile.language)}
            >
              {profile.parentName}
            </DiscreetLink>
          </Note>
        )
      : null;

    return (
      <TableRowInteractive key={profile.key}>
        <ContentCell className="coding-rules-detail-quality-profile-name">
          <div className="sw-flex sw-flex-col">
            <div>
              <Link
                aria-label={profile.name}
                to={getQualityProfileUrl(profile.name, profile.language)}
              >
                {profile.name}
              </Link>
              {profile.isBuiltIn && <BuiltInQualityProfileBadge className="sw-ml-2" />}
            </div>
            {inheritedProfileSection}
          </div>
        </ContentCell>

        {!ruleDetails.templateKey && (
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
      <SubHeadingHighlight as="h2" id={PROFILES_HEADING_ID}>
        <FormattedMessage id="coding_rules.quality_profiles" />
      </SubHeadingHighlight>

      {canActivate && (
        <ActivationButton
          buttonText={translate('coding_rules.activate')}
          className="sw-mt-6"
          modalHeader={translate('coding_rules.activate_in_quality_profile')}
          onDone={props.onActivate}
          profiles={filter(
            referencedProfiles,
            (profile) => !activations.find((activation) => activation.qProfile === profile.key),
          )}
          rule={ruleDetails}
        />
      )}

      {activations.length > 0 && (
        <Table
          aria-labelledby={PROFILES_HEADING_ID}
          className="sw-my-6"
          columnCount={
            ruleDetails.templateKey ? COLUMN_COUNT_WITHOUT_PARAMS : COLUMN_COUNT_WITH_PARAMS
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
