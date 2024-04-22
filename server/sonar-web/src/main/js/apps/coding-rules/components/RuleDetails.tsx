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
  ButtonSecondary,
  DangerButtonSecondary,
  HelperHintIcon,
  Spinner,
  SubHeadingHighlight,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import DateFormatter from '../../../components/intl/DateFormatter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import {
  useDeleteRuleMutation,
  useRuleDetailsQuery,
  useUpdateRuleMutation,
} from '../../../queries/rules';
import HelpTooltip from '../../../sonar-aligned/components/controls/HelpTooltip';
import { Dict } from '../../../types/types';
import { Activation } from '../query';
import CustomRuleButton from './CustomRuleButton';
import RuleDetailsCustomRules from './RuleDetailsCustomRules';
import RuleDetailsDescription from './RuleDetailsDescription';
import RuleDetailsHeader from './RuleDetailsHeader';
import RuleDetailsIssues from './RuleDetailsIssues';
import RuleDetailsParameters from './RuleDetailsParameters';
import RuleDetailsProfiles from './RuleDetailsProfiles';

interface Props {
  allowCustomRules?: boolean;
  canWrite?: boolean;
  canDeactivateInherited?: boolean;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onDelete: (rule: string) => void;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleKey: string;
  selectedProfile?: Profile;
}

export default function RuleDetails(props: Readonly<Props>) {
  const {
    ruleKey,
    allowCustomRules,
    canWrite,
    referencedProfiles,
    canDeactivateInherited,
    selectedProfile,
    referencedRepositories,
  } = props;
  const { isLoading: loadingRule, data } = useRuleDetailsQuery({
    actives: true,
    key: ruleKey,
  });
  const { mutate: updateRule } = useUpdateRuleMutation();
  const { mutate: deleteRule } = useDeleteRuleMutation({}, props.onDelete);

  const { rule: ruleDetails, actives = [] } = data ?? {};

  const params = ruleDetails?.params ?? [];
  const isCustom = !!ruleDetails?.templateKey;
  const isEditable = canWrite && !!allowCustomRules && isCustom;

  const handleTagsChange = (tags: string[]) => {
    updateRule({ key: ruleKey, tags: tags.join() });
  };

  const handleActivate = () => {
    if (selectedProfile) {
      const active = actives.find((active) => active.qProfile === selectedProfile.key);
      if (active) {
        props.onActivate(selectedProfile.key, ruleKey, active);
      }
    }
  };

  const handleDeactivate = () => {
    if (selectedProfile && actives.find((active) => active.qProfile === selectedProfile.key)) {
      props.onDeactivate(selectedProfile.key, ruleKey);
    }
  };

  return (
    <StyledRuleDetails className="it__coding-rule-details sw-p-6 sw-mt-6">
      <Spinner loading={loadingRule}>
        {ruleDetails && (
          <>
            <RuleDetailsHeader
              canWrite={canWrite}
              onTagsChange={handleTagsChange}
              referencedRepositories={referencedRepositories}
              ruleDetails={ruleDetails}
            />

            <RuleDetailsDescription canWrite={canWrite} ruleDetails={ruleDetails} />

            {params.length > 0 && <RuleDetailsParameters params={params} />}

            {isEditable && (
              <div className="coding-rules-detail-description sw-flex sw-items-center">
                {/* `templateRule` is used to get rule meta data, `customRule` is used to get parameter values */}
                {/* it's expected to pass the same rule to both parameters */}
                <CustomRuleButton customRule={ruleDetails} templateRule={ruleDetails}>
                  {({ onClick }) => (
                    <ButtonSecondary
                      className="js-edit-custom"
                      id="coding-rules-detail-custom-rule-change"
                      onClick={onClick}
                    >
                      {translate('edit')}
                    </ButtonSecondary>
                  )}
                </CustomRuleButton>
                <ConfirmButton
                  confirmButtonText={translate('delete')}
                  isDestructive
                  modalBody={translateWithParameters(
                    'coding_rules.delete.custom.confirm',
                    ruleDetails.name,
                  )}
                  modalHeader={translate('coding_rules.delete_rule')}
                  onConfirm={() => deleteRule({ key: ruleKey })}
                >
                  {({ onClick }) => (
                    <>
                      <DangerButtonSecondary
                        className="sw-ml-2 js-delete"
                        id="coding-rules-detail-rule-delete"
                        onClick={onClick}
                      >
                        {translate('delete')}
                      </DangerButtonSecondary>
                      <HelpTooltip
                        className="sw-ml-2"
                        overlay={
                          <div className="sw-py-4">
                            {translate('coding_rules.custom_rule.removal')}
                          </div>
                        }
                      >
                        <HelperHintIcon />
                      </HelpTooltip>
                    </>
                  )}
                </ConfirmButton>
              </div>
            )}

            {ruleDetails.isTemplate && (
              <RuleDetailsCustomRules
                canChange={allowCustomRules && canWrite}
                ruleDetails={ruleDetails}
              />
            )}

            {!ruleDetails.isTemplate && (
              <RuleDetailsProfiles
                activations={actives}
                canDeactivateInherited={canDeactivateInherited}
                onActivate={handleActivate}
                onDeactivate={handleDeactivate}
                referencedProfiles={referencedProfiles}
                ruleDetails={ruleDetails}
              />
            )}

            {!ruleDetails.isTemplate && ruleDetails.type !== 'SECURITY_HOTSPOT' && (
              <RuleDetailsIssues ruleDetails={ruleDetails} />
            )}

            <div className="sw-my-8" data-meta="available-since">
              <SubHeadingHighlight as="h3">
                {translate('coding_rules.available_since')}
              </SubHeadingHighlight>
              <DateFormatter date={ruleDetails.createdAt} />
            </div>
          </>
        )}
      </Spinner>
    </StyledRuleDetails>
  );
}

const StyledRuleDetails = styled.div`
  box-sizing: border-box;
  border-radius: 4px;
  background-color: ${themeColor('filterbar')};
  border: ${themeBorder('default', 'filterbarBorder')};
  overflow-x: hidden;
`;
