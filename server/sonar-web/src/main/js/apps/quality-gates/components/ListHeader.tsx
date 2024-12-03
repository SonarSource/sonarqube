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
  Button,
  ButtonVariety,
  Heading,
  IconQuestionMark,
  Popover,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { useIntl } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import CreateQualityGateForm from './CreateQualityGateForm';

interface Props {
  canCreate: boolean;
}

function CreateQualityGateModal() {
  const renderModal = React.useCallback(
    ({ onClose }: ModalProps) => <CreateQualityGateForm onClose={onClose} />,
    [],
  );

  return (
    <div>
      <ModalButton modal={renderModal}>
        {({ onClick }) => (
          <Button data-test="quality-gates__add" onClick={onClick} variety={ButtonVariety.Default}>
            {translate('create')}
          </Button>
        )}
      </ModalButton>
    </div>
  );
}

export default function ListHeader({ canCreate }: Readonly<Props>) {
  const intl = useIntl();
  return (
    <div className="sw-flex sw-justify-between sw-pb-4">
      <div className="sw-flex sw-items-center sw-gap-1 sw-justify-between">
        <Heading as="h1" className="sw-flex sw-items-center sw-typo-lg-semibold sw-mb-0">
          {intl.formatMessage({ id: 'quality_gates.page' })}
        </Heading>
        <Popover
          title={intl.formatMessage({ id: 'quality_gates.help.title' })}
          description={intl.formatMessage({ id: 'quality_gates.help.desc' })}
          footer={
            <DocumentationLink shouldOpenInNewTab standalone to={DocLink.QualityGates}>
              {intl.formatMessage({ id: 'quality_gates.help.link' })}
            </DocumentationLink>
          }
        >
          <Button
            className="sw-p-0 sw-h-fit sw-min-h-fit"
            aria-label={intl.formatMessage({ id: 'help' })}
            variety={ButtonVariety.DefaultGhost}
          >
            <IconQuestionMark />
          </Button>
        </Popover>
      </div>
      {canCreate && <CreateQualityGateModal />}
    </div>
  );
}
