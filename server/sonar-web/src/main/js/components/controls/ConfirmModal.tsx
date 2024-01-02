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
import { ButtonPrimary, DangerButtonPrimary, Modal } from 'design-system';
import React from 'react';
import { translate } from '../../helpers/l10n';
import ClickEventBoundary from './ClickEventBoundary';

export interface ConfirmModalProps<T> {
  cancelButtonText?: string;
  children: React.ReactNode;
  confirmButtonText: string;
  confirmData?: T;
  confirmDisable?: boolean;
  isDestructive?: boolean;
  onConfirm: (data?: T) => void | Promise<void | Response>;
}

interface Props<T> extends ConfirmModalProps<T> {
  header: string;
  headerDescription?: React.ReactNode;
  onClose: () => void;
}

export default function ConfirmModal<T = string>(props: Readonly<Props<T>>) {
  const {
    header,
    onClose,
    onConfirm,
    children,
    confirmButtonText,
    confirmData,
    confirmDisable,
    headerDescription,
    isDestructive,
    cancelButtonText = translate('cancel'),
  } = props;

  const [submitting, setSubmitting] = React.useState(false);

  const handleSubmit = React.useCallback(() => {
    setSubmitting(true);
    const result = onConfirm(confirmData);

    if (result) {
      return result.then(
        () => {
          onClose();
        },
        () => {
          /* noop */
        },
      );
    }

    onClose();
    return undefined;
  }, [confirmData, onClose, onConfirm, setSubmitting]);

  const Button = isDestructive ? DangerButtonPrimary : ButtonPrimary;

  return (
    <Modal
      headerTitle={header}
      headerDescription={headerDescription}
      body={
        <ClickEventBoundary>
          <>{children}</>
        </ClickEventBoundary>
      }
      primaryButton={
        <Button autoFocus disabled={submitting || confirmDisable} onClick={handleSubmit}>
          {confirmButtonText}
        </Button>
      }
      secondaryButtonLabel={cancelButtonText}
      loading={submitting}
      onClose={onClose}
    />
  );
}
