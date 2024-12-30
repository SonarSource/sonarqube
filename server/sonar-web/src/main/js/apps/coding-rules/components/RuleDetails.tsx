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
  Button,
  ButtonVariety,
  Heading,
  IconQuestionMark,
  ModalAlert,
  Spinner,
} from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import { themeBorder, themeColor } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { Profile } from '../../../api/quality-profiles';
import DateFormatter from '../../../components/intl/DateFormatter';
import { translate } from '../../../helpers/l10n';
import {
  useDeleteRuleMutation,
  useRuleDetailsQuery,
  useUpdateRuleMutation,
} from '../../../queries/rules';
import { Dict, RuleActivation } from '../../../types/types';
import CustomRuleButton from './CustomRuleButton';
import RuleDetailsCustomRules from './RuleDetailsCustomRules';
import RuleDetailsDescription from './RuleDetailsDescription';
import RuleDetailsHeader from './RuleDetailsHeader';
import RuleDetailsIssues from './RuleDetailsIssues';
import RuleDetailsParameters from './RuleDetailsParameters';
import RuleDetailsProfiles from './RuleDetailsProfiles';

interface Props {
  organization: string;
  allowCustomRules?: boolean;
  canDeactivateInherited?: boolean;
  canWrite?: boolean;
  onActivate: (profile: string, rule: string, activation: RuleActivation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onDelete: (rule: string) => void;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleKey: string;
  selectedProfile?: Profile;
}

export default function RuleDetails(props: Readonly<Props>) {
  const {
    organization,
    ruleKey,
    allowCustomRules,
    canWrite,
    referencedProfiles,
    canDeactivateInherited,
    selectedProfile,
    referencedRepositories,
  } = props;
  const intl = useIntl();
  const { isLoading: loadingRule, data } = useRuleDetailsQuery({
    organization,
    actives: true,
    key: ruleKey,
  });
  const { mutate: updateRule } = useUpdateRuleMutation();
  const { mutate: deleteRule } = useDeleteRuleMutation({}, props.onDelete);

  const { rule: ruleDetails, actives = [] } = data ?? {};

  const params = ruleDetails?.params ?? [];
  const isCustom = ruleDetails?.templateKey !== undefined;
  const isEditable = canWrite && !!allowCustomRules && isCustom;

  const handleTagsChange = (tags: string[]) => {
    updateRule({ organization, key: ruleKey, tags: tags.join() });
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
      <Spinner isLoading={loadingRule}>
        {ruleDetails && (
          <>
            <RuleDetailsHeader
              canWrite={canWrite}
              onTagsChange={handleTagsChange}
              referencedRepositories={referencedRepositories}
              ruleDetails={ruleDetails}
              organization={organization}
            />

            <RuleDetailsDescription organization={organization} canWrite={canWrite} ruleDetails={ruleDetails} />

            {params.length > 0 && <RuleDetailsParameters params={params} />}

            {isEditable && (
              <div className="coding-rules-detail-description sw-flex sw-items-center">
                {/* `templateRule` is used to get rule meta data, `customRule` is used to get parameter values */}
                {/* it's expected to pass the same rule to both parameters */}
                <CustomRuleButton organization={organization} customRule={ruleDetails} templateRule={ruleDetails}>
                  {({ onClick }) => (
                    <Button
                      variety={ButtonVariety.Default}
                      className="js-edit-custom"
                      id="coding-rules-detail-custom-rule-change"
                      onClick={onClick}
                    >
                      {translate('edit')}
                    </Button>
                  )}
                </CustomRuleButton>
                <ModalAlert
                  title={translate('coding_rules.delete_rule')}
                  description={intl.formatMessage(
                    {
                      id: 'coding_rules.delete.custom.confirm',
                    },
                    {
                      name: ruleDetails.name,
                    },
                  )}
                  primaryButton={
                    <Button
                      className="sw-ml-2 js-delete"
                      id="coding-rules-detail-rule-delete"
                      onClick={() => deleteRule({ key: ruleKey })}
                      variety={ButtonVariety.DangerOutline}
                    >
                      {translate('delete')}
                    </Button>
                  }
                  secondaryButtonLabel={translate('close')}
                >
                  <Button
                    className="sw-ml-2 js-delete"
                    id="coding-rules-detail-rule-delete"
                    variety={ButtonVariety.DangerOutline}
                  >
                    {translate('delete')}
                  </Button>
                </ModalAlert>
                <HelpTooltip
                  className="sw-ml-2"
                  overlay={
                    <div className="sw-py-4">{translate('coding_rules.custom_rule.removal')}</div>
                  }
                >
                  <IconQuestionMark />
                </HelpTooltip>
              </div>
            )}

            {ruleDetails.isTemplate && (
              <RuleDetailsCustomRules
                organization={organization}
                canChange={allowCustomRules && canWrite}
                ruleDetails={ruleDetails}
              />
            )}

            {!ruleDetails.isTemplate && (
              <RuleDetailsProfiles
                organization={organization}
                activations={actives}
                canDeactivateInherited={canDeactivateInherited}
                onActivate={handleActivate}
                onDeactivate={handleDeactivate}
                referencedProfiles={referencedProfiles}
                ruleDetails={ruleDetails}
              />
            )}

            {!ruleDetails.isTemplate && ruleDetails.type !== 'SECURITY_HOTSPOT' && (
              <RuleDetailsIssues organization={organization} ruleDetails={ruleDetails} />
            )}

            <div className="sw-my-8" data-meta="available-since">
              <Heading as="h3">{translate('coding_rules.available_since')}</Heading>
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
