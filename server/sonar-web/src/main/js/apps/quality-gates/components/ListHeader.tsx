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
import { ButtonPrimary, HelperHintIcon, Title } from 'design-system';
import * as React from 'react';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { translate } from '../../../helpers/l10n';
import CreateQualityGateForm from './CreateQualityGateForm';

interface Props {
  canCreate: boolean;
  refreshQualityGates: () => Promise<void>;
}

function CreateQualityGateModal({
  refreshQualityGates,
}: Readonly<Pick<Props, 'refreshQualityGates'>>) {
  const renderModal = React.useCallback(
    ({ onClose }: ModalProps) => (
      <CreateQualityGateForm onClose={onClose} onCreate={refreshQualityGates} />
    ),
    [refreshQualityGates],
  );

  return (
    <div>
      <ModalButton modal={renderModal}>
        {({ onClick }) => (
          <ButtonPrimary data-test="quality-gates__add" onClick={onClick}>
            {translate('create')}
          </ButtonPrimary>
        )}
      </ModalButton>
    </div>
  );
}

export default function ListHeader({ canCreate, refreshQualityGates }: Readonly<Props>) {
  return (
    <div className="sw-flex sw-justify-between sw-pb-4">
      <div className="sw-flex sw-justify-between">
        <Title className="sw-flex sw-items-center sw-body-md-highlight sw-mb-0">
          {translate('quality_gates.page')}
        </Title>
        <DocumentationTooltip
          className="sw-ml-2"
          content={translate('quality_gates.help')}
          links={[
            {
              href: '/user-guide/quality-gates/',
              label: translate('learn_more'),
            },
          ]}
        >
          <HelperHintIcon />
        </DocumentationTooltip>
      </div>
      {canCreate && <CreateQualityGateModal refreshQualityGates={refreshQualityGates} />}
    </div>
  );
}
