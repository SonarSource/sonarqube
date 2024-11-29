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

import { Button, ButtonVariety, Modal, ModalSize } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';

interface Props {
  count: number;
  isDefault?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export default function DisqualifyAiQualityGateForm({
  onConfirm,
  onClose,
  isDefault = false,
  count = 0,
}: Readonly<Props>) {
  const intl = useIntl();

  return (
    <Modal
      isOpen
      size={ModalSize.Wide}
      title={
        isDefault
          ? intl.formatMessage({ id: 'quality_gates.disqualify_ai_modal_default.title' })
          : intl.formatMessage({ id: 'quality_gates.disqualify_ai_modal.title' }, { count })
      }
      onOpenChange={onClose}
      content={
        <>
          <p>
            {isDefault
              ? intl.formatMessage(
                  { id: 'quality_gates.disqualify_ai_modal_default.content.line1' },
                  { count },
                )
              : intl.formatMessage(
                  { id: 'quality_gates.disqualify_ai_modal.content.line1' },
                  { count },
                )}
          </p>
          <br />
          <p>{intl.formatMessage({ id: 'quality_gates.disqualify_ai_modal.content.line2' })}</p>
        </>
      }
      primaryButton={
        <Button onClick={onConfirm} variety={ButtonVariety.Primary}>
          {intl.formatMessage({ id: 'quality_gates.disqualify_ai_modal.confirm' })}
        </Button>
      }
      secondaryButton={
        <Button hasAutoFocus onClick={onClose}>
          {intl.formatMessage({ id: 'cancel' })}
        </Button>
      }
    />
  );
}
