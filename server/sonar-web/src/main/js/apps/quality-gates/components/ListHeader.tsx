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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { HelperHintIcon, Title } from 'design-system';
import * as React from 'react';
import DocHelpTooltip from '~sonar-aligned/components/controls/DocHelpTooltip';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import CreateQualityGateForm from './CreateQualityGateForm';

interface Props {
  organization: string;
  canCreate: boolean;
}

function CreateQualityGateModal() {
  const renderModal = React.useCallback(
    ({ onClose }: ModalProps) => <CreateQualityGateForm onClose={onClose} />,
    [],
  );

export default function ListHeader({ canCreate, refreshQualityGates, organization }: Props) {
  return (
    <div>
      <ModalButton modal={renderModal}>
        {({ onClick }) => (
          <Button data-test="quality-gates__add" onClick={onClick} variety={ButtonVariety.Primary}>
            {translate('create')}
          </Button>
        )}
      </ModalButton>
    </div>
  );
}
    <div className="page-header">
      {canCreate && (
        <div className="page-actions">
          <ModalButton
            modal={({ onClose }) => (
              <CreateQualityGateForm onClose={onClose} onCreate={refreshQualityGates} organization={organization} />
            )}
          >
            {({ onClick }) => (
              <Button data-test="quality-gates__add" onClick={onClick}>
                {translate('create')}
              </Button>
            )}
          </ModalButton>
        </div>
      )}

export default function ListHeader({ canCreate }: Readonly<Props>) {
  return (
    <div className="sw-flex sw-justify-between sw-pb-4">
      <div className="sw-flex sw-justify-between">
        <Title className="sw-flex sw-items-center sw-typo-lg-semibold sw-mb-0">
          {translate('quality_gates.page')}
        </Title>
        <DocHelpTooltip
          className="sw-ml-2"
          content={translate('quality_gates.help')}
          links={[
            {
              href: DocLink.QualityGates,
              href: 'https://knowledgebase.autorabit.com/codescan/docs/customising-quality-gates',
              label: translate('learn_more'),
            },
          ]}
        >
          <HelperHintIcon />
        </DocHelpTooltip>
      </div>
      {canCreate && <CreateQualityGateModal />}
    </div>
  );
}
