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
import { Profile, activateRule, deactivateRule } from '../../../api/quality-profiles';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import { Dict, RuleActivation, RuleDetails } from '../../../types/types';
import BuiltInQualityProfileBadge from '../../quality-profiles/components/BuiltInQualityProfileBadge';
import ActivationButton from './ActivationButton';

interface Props {
  activations: RuleActivation[] | undefined;
  canDeactivateInherited?: boolean;
  onActivate: () => Promise<void>;
  onDeactivate: () => Promise<void>;
  referencedProfiles: Dict<Profile>;
  ruleDetails: RuleDetails;
}

const COLUMN_COUNT_WITH_PARAMS = 3;
const COLUMN_COUNT_WITHOUT_PARAMS = 2;

const PROFILES_HEADING_ID = 'rule-details-profiles-heading';

export default class RuleDetailsProfiles extends React.PureComponent<Props> {
  handleActivate = () => this.props.onActivate();

  handleDeactivate = (key?: string) => {
    if (key !== undefined) {
      deactivateRule({
        key,
        rule: this.props.ruleDetails.key,
      }).then(this.props.onDeactivate, () => {});
    }
  };

  handleRevert = (key?: string) => {
    if (key !== undefined) {
      activateRule({
        key,
        rule: this.props.ruleDetails.key,
        reset: true,
      }).then(this.props.onActivate, () => {});
    }
  };

  renderInheritedProfile = (activation: RuleActivation, profile: Profile) => {
    if (!profile.parentName) {
      return null;
    }
    const profilePath = getQualityProfileUrl(profile.parentName, profile.language);
    return (
      (activation.inherit === 'OVERRIDES' || activation.inherit === 'INHERITED') && (
        <Note as="div" className="sw-flex sw-items-center sw-mt-2">
          <InheritanceIcon
            fill={activation.inherit === 'OVERRIDES' ? 'destructiveIconFocus' : 'currentColor'}
          />
          <DiscreetLink
            className="sw-ml-1"
            aria-label={`${translate('quality_profiles.parent')} ${profile.parentName}`}
            to={profilePath}
          >
            {profile.parentName}
          </DiscreetLink>
        </Note>
      )
    );
  };

  renderParameter = (param: { key: string; value: string }, parentActivation?: RuleActivation) => {
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
  };

  renderParameters = (activation: RuleActivation, parentActivation?: RuleActivation) => (
    <CellComponent>
      {activation.params.map((param) => this.renderParameter(param, parentActivation))}
    </CellComponent>
  );

  renderActions = (activation: RuleActivation, profile: Profile) => {
    const canEdit = profile.actions?.edit && !profile.isBuiltIn;
    const { ruleDetails } = this.props;
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
                onDone={this.handleActivate}
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
                onConfirm={this.handleRevert}
              >
                {({ onClick }) => (
                  <DangerButtonSecondary className="sw-ml-2" onClick={onClick}>
                    {translate('coding_rules.revert_to_parent_definition')}
                  </DangerButtonSecondary>
                )}
              </ConfirmButton>
            )}

            {(!hasParent || this.props.canDeactivateInherited) && (
              <ConfirmButton
                confirmButtonText={translate('yes')}
                confirmData={profile.key}
                modalBody={translate('coding_rules.deactivate.confirm')}
                modalHeader={translate('coding_rules.deactivate')}
                onConfirm={this.handleDeactivate}
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

  renderActivation = (activation: RuleActivation) => {
    const { activations = [], ruleDetails } = this.props;
    const profile = this.props.referencedProfiles[activation.qProfile];
    if (!profile) {
      return null;
    }

    const parentActivation = activations.find((x) => x.qProfile === profile.parentKey);

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
            {this.renderInheritedProfile(activation, profile)}
          </div>
        </ContentCell>

        {!ruleDetails.templateKey && this.renderParameters(activation, parentActivation)}
        {this.renderActions(activation, profile)}
      </TableRowInteractive>
    );
  };

  render() {
    const { activations = [], referencedProfiles, ruleDetails } = this.props;
    const canActivate = Object.values(referencedProfiles).some((profile) =>
      Boolean(profile.actions?.edit && profile.language === ruleDetails.lang),
    );

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
            onDone={this.handleActivate}
            profiles={filter(
              this.props.referencedProfiles,
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
            {activations.map(this.renderActivation)}
          </Table>
        )}
      </div>
    );
  }
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
