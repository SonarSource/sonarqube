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

import { DangerButtonSecondary } from '~design-system';
import { Profile } from '../../../api/quality-profiles';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Rule, RuleActivation } from '../../../types/types';
import ActivationButton from './ActivationButton';

interface Props {
  activation: RuleActivation;
  canDeactivateInherited?: boolean;
  handleDeactivate: (key?: string) => void;
  handleRevert: (key?: string) => void;
  onActivate: (severity: string, prioritizedRule: boolean) => Promise<void> | void;
  profile: Profile;
  ruleDetails: Rule;
  showDeactivated?: boolean;
  organization: string;
}

export default function ActivatedRuleActions(props: Readonly<Props>) {
  const {
    activation,
    profile,
    ruleDetails,
    onActivate,
    handleRevert,
    handleDeactivate,
    showDeactivated,
    canDeactivateInherited,
    organization
  } = props;

  const canEdit = profile.actions?.edit && !profile.isBuiltIn;
  const hasParent = activation.inherit !== 'NONE' && profile.parentKey !== undefined;

  return (
    <>
      {canEdit && (
        <>
          {!ruleDetails.isTemplate && (
            <ActivationButton
              className="sw-ml-2"
              activation={activation}
              ariaLabel={translateWithParameters('coding_rules.change_details_x', profile.name)}
              buttonText={translate('change_verb')}
              modalHeader={translate('coding_rules.change_details')}
              onDone={onActivate}
              profiles={[profile]}
              organization={organization}
              rule={ruleDetails}
            />
          )}

          {hasParent && activation.inherit === 'OVERRIDES' && profile.parentName && (
            <ConfirmButton
              confirmButtonText={translate('yes')}
              confirmData={profile.key}
              isDestructive
              modalBody={translateWithParameters(
                'coding_rules.revert_to_parent_definition.confirm',
                profile.parentName,
              )}
              modalHeader={translate('coding_rules.revert_to_parent_definition')}
              onConfirm={handleRevert}
            >
              {({ onClick }) => (
                <DangerButtonSecondary className="sw-ml-2 sw-whitespace-nowrap" onClick={onClick}>
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
                  className="sw-ml-2 sw-whitespace-nowrap"
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

          {showDeactivated &&
            hasParent &&
            !canDeactivateInherited &&
            activation.inherit !== 'OVERRIDES' && (
              <Tooltip content={translate('coding_rules.can_not_deactivate')}>
                <DangerButtonSecondary
                  disabled
                  className="sw-ml-2"
                  aria-label={translateWithParameters(
                    'coding_rules.deactivate_in_quality_profile_x',
                    profile.name,
                  )}
                >
                  {translate('coding_rules.deactivate')}
                </DangerButtonSecondary>
              </Tooltip>
            )}
        </>
      )}
    </>
  );
}
