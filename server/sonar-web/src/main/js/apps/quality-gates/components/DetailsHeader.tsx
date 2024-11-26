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

import {
  ButtonIcon,
  DropdownMenu,
  DropdownMenuAlign,
  IconMoreVertical,
  Tooltip,
} from '@sonarsource/echoes-react';
import { countBy } from 'lodash';
import * as React from 'react';
import { useCallback } from 'react';
import { Badge, ButtonSecondary, DangerButtonPrimary, SubTitle } from '~design-system';
import { AiCodeAssuranceStatus } from '../../../api/ai-code-assurance';
import LegacyTooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import {
  useGetAllQualityGateProjectsQuery,
  useSetAiSupportedQualityGateMutation,
  useSetQualityGateAsDefaultMutation,
} from '../../../queries/quality-gates';
import { CaycStatus, QualityGate } from '../../../types/types';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
import CopyQualityGateForm from './CopyQualityGateForm';
import DeleteQualityGateForm from './DeleteQualityGateForm';
import DisqualifyAiQualityGateForm from './DisqualifyAiQualityGateForm';
import RenameQualityGateForm from './RenameQualityGateForm';

interface Props {
  qualityGate: QualityGate;
}

export default function DetailsHeader({ qualityGate }: Readonly<Props>) {
  const [isRenameFormOpen, setIsRenameFormOpen] = React.useState(false);
  const [isCopyFormOpen, setIsCopyFormOpen] = React.useState(false);
  const [isRemoveFormOpen, setIsRemoveFormOpen] = React.useState(false);
  const [isQualifyAiFormOpen, setIsQualifyAiFormOpen] = React.useState(false);
  const actions = qualityGate.actions ?? {};
  const actionsCount = countBy([
    actions.rename,
    actions.copy,
    actions.delete,
    actions.setAsDefault,
  ])['true'];
  const { mutateAsync: setQualityGateAsDefault } = useSetQualityGateAsDefaultMutation();
  const { mutateAsync: setAiSupportedQualityGate } = useSetAiSupportedQualityGateMutation(
    qualityGate.name,
  );
  const { data: qualityGateProjectsHavingAiCode = [], isLoading: isCountLoading } =
    useGetAllQualityGateProjectsQuery(
      { gateName: qualityGate.name, selected: 'selected' },
      {
        select: (data) =>
          data.results.filter((p) => p.aiCodeAssurance === AiCodeAssuranceStatus.AI_CODE_ASSURED),
      },
    );

  const handleSetAsDefaultClick = () => {
    if (!qualityGate.isDefault) {
      setQualityGateAsDefault({ name: qualityGate.name });
    }
  };

  const handleSetQualityGateAiCodeAssurance = () => {
    if (qualityGateProjectsHavingAiCode?.length > 0 && qualityGate.isAiCodeSupported) {
      setIsQualifyAiFormOpen(true);
      return;
    }

    updateQualityGateAiCodeAssurance();
  };

  const updateQualityGateAiCodeAssurance = useCallback(() => {
    setAiSupportedQualityGate({
      isQualityGateAiSupported: !qualityGate.isAiCodeSupported,
      name: qualityGate.name,
    });
    setIsQualifyAiFormOpen(false);
  }, [qualityGate.isAiCodeSupported, qualityGate.name, setAiSupportedQualityGate]);

  return (
    <>
      <div className="it__layout-page-main-header sw-flex sw-items-center sw-justify-between sw-mb-9">
        <div className="sw-flex sw-flex-col">
          <div className="sw-flex sw-items-baseline">
            <SubTitle className="sw-m-0">{qualityGate.name}</SubTitle>
            <div className="sw-flex sw-gap-2 sw-ml-4">
              {qualityGate.isDefault && <Badge>{translate('default')}</Badge>}
              {qualityGate.isBuiltIn && <BuiltInQualityGateBadge />}
            </div>
          </div>
        </div>
        {actionsCount === 1 && (
          <>
            {actions.rename && (
              <ButtonSecondary onClick={() => setIsRenameFormOpen(true)}>
                {translate('rename')}
              </ButtonSecondary>
            )}
            {actions.copy && (
              <LegacyTooltip
                content={
                  qualityGate.caycStatus === CaycStatus.NonCompliant
                    ? translate('quality_gates.cannot_copy_no_cayc')
                    : null
                }
              >
                <ButtonSecondary
                  disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                  onClick={() => setIsCopyFormOpen(true)}
                >
                  {translate('copy')}
                </ButtonSecondary>
              </LegacyTooltip>
            )}
            {actions.setAsDefault && (
              <LegacyTooltip
                content={
                  qualityGate.caycStatus === CaycStatus.NonCompliant
                    ? translate('quality_gates.cannot_set_default_no_cayc')
                    : null
                }
              >
                <ButtonSecondary
                  disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                  onClick={handleSetAsDefaultClick}
                >
                  {translate('set_as_default')}
                </ButtonSecondary>
              </LegacyTooltip>
            )}
            {actions.delete && (
              <DangerButtonPrimary onClick={() => setIsRemoveFormOpen(true)}>
                {translate('delete')}
              </DangerButtonPrimary>
            )}
          </>
        )}

        {actionsCount > 1 && (
          <DropdownMenu.Root
            align={DropdownMenuAlign.End}
            id="quality-gate-actions"
            items={
              <>
                {actions.rename && (
                  <DropdownMenu.ItemButton onClick={() => setIsRenameFormOpen(true)}>
                    {translate('rename')}
                  </DropdownMenu.ItemButton>
                )}
                {actions.copy && (
                  <Tooltip
                    content={
                      qualityGate.caycStatus === CaycStatus.NonCompliant
                        ? translate('quality_gates.cannot_copy_no_cayc')
                        : null
                    }
                  >
                    <DropdownMenu.ItemButton
                      isDisabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                      onClick={() => setIsCopyFormOpen(true)}
                    >
                      {translate('copy')}
                    </DropdownMenu.ItemButton>
                  </Tooltip>
                )}
                {actions.setAsDefault && (
                  <Tooltip
                    content={
                      qualityGate.caycStatus === CaycStatus.NonCompliant
                        ? translate('quality_gates.cannot_set_default_no_cayc')
                        : null
                    }
                  >
                    <DropdownMenu.ItemButton
                      isDisabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                      onClick={handleSetAsDefaultClick}
                    >
                      {translate('set_as_default')}
                    </DropdownMenu.ItemButton>
                  </Tooltip>
                )}
                {actions.manageAiCodeAssurance && !isCountLoading && (
                  <DropdownMenu.ItemButton onClick={handleSetQualityGateAiCodeAssurance}>
                    {translate(
                      qualityGate.isAiCodeSupported
                        ? 'quality_gates.actions.disqualify_for_ai_code_assurance'
                        : 'quality_gates.actions.qualify_for_ai_code_assurance',
                    )}
                  </DropdownMenu.ItemButton>
                )}
                {actions.delete && (
                  <>
                    <DropdownMenu.Separator />
                    <DropdownMenu.ItemButtonDestructive onClick={() => setIsRemoveFormOpen(true)}>
                      {translate('delete')}
                    </DropdownMenu.ItemButtonDestructive>
                  </>
                )}
              </>
            }
          >
            <ButtonIcon Icon={IconMoreVertical} ariaLabel={translate('actions')} />
          </DropdownMenu.Root>
        )}
      </div>

      {isRenameFormOpen && (
        <RenameQualityGateForm
          onClose={() => setIsRenameFormOpen(false)}
          qualityGate={qualityGate}
        />
      )}

      {isCopyFormOpen && (
        <CopyQualityGateForm onClose={() => setIsCopyFormOpen(false)} qualityGate={qualityGate} />
      )}

      {isRemoveFormOpen && (
        <DeleteQualityGateForm
          onClose={() => setIsRemoveFormOpen(false)}
          qualityGate={qualityGate}
        />
      )}
      {isQualifyAiFormOpen && (
        <DisqualifyAiQualityGateForm
          onClose={() => setIsQualifyAiFormOpen(false)}
          onConfirm={updateQualityGateAiCodeAssurance}
          count={qualityGateProjectsHavingAiCode.length}
        />
      )}
    </>
  );
}
