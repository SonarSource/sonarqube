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
  ActionsDropdown,
  Badge,
  ButtonSecondary,
  DangerButtonPrimary,
  ItemButton,
  ItemDangerButton,
  ItemDivider,
  SubTitle,
} from 'design-system';
import { countBy } from 'lodash';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { useSetQualityGateAsDefaultMutation } from '../../../queries/quality-gates';
import { CaycStatus, QualityGate } from '../../../types/types';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
import CopyQualityGateForm from './CopyQualityGateForm';
import DeleteQualityGateForm from './DeleteQualityGateForm';
import RenameQualityGateForm from './RenameQualityGateForm';

interface Props {
  qualityGate: QualityGate;
}

export default function DetailsHeader({ qualityGate }: Readonly<Props>) {
  const [isRenameFormOpen, setIsRenameFormOpen] = React.useState(false);
  const [isCopyFormOpen, setIsCopyFormOpen] = React.useState(false);
  const [isRemoveFormOpen, setIsRemoveFormOpen] = React.useState(false);
  const actions = qualityGate.actions ?? {};
  const actionsCount = countBy([
    actions.rename,
    actions.copy,
    actions.delete,
    actions.setAsDefault,
  ])['true'];
  const { mutateAsync: setQualityGateAsDefault } = useSetQualityGateAsDefaultMutation();

  const handleSetAsDefaultClick = () => {
    if (!qualityGate.isDefault) {
      setQualityGateAsDefault({ name: qualityGate.name });
    }
  };

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
              <Tooltip
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
                <ButtonSecondary
                  disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                  onClick={handleSetAsDefaultClick}
                >
                  {translate('set_as_default')}
                </ButtonSecondary>
              </Tooltip>
            )}
            {actions.delete && (
              <DangerButtonPrimary onClick={() => setIsRemoveFormOpen(true)}>
                {translate('delete')}
              </DangerButtonPrimary>
            )}
          </>
        )}

        {actionsCount > 1 && (
          <ActionsDropdown allowResizing id="quality-gate-actions">
            {actions.rename && (
              <ItemButton onClick={() => setIsRenameFormOpen(true)}>
                {translate('rename')}
              </ItemButton>
            )}
            {actions.copy && (
              <Tooltip
                content={
                  qualityGate.caycStatus === CaycStatus.NonCompliant
                    ? translate('quality_gates.cannot_copy_no_cayc')
                    : null
                }
              >
                <ItemButton
                  disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                  onClick={() => setIsCopyFormOpen(true)}
                >
                  {translate('copy')}
                </ItemButton>
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
                <ItemButton
                  disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                  onClick={handleSetAsDefaultClick}
                >
                  {translate('set_as_default')}
                </ItemButton>
              </Tooltip>
            )}
            {actions.delete && (
              <>
                <ItemDivider />
                <ItemDangerButton onClick={() => setIsRemoveFormOpen(true)}>
                  {translate('delete')}
                </ItemDangerButton>
              </>
            )}
          </ActionsDropdown>
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
    </>
  );
}
