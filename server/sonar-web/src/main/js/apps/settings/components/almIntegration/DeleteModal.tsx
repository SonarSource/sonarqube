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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export interface DeleteModalProps {
  id: string;
  projectCount?: number;
  onDelete: (id: string) => void;
  onCancel: () => void;
}

function showProjectCountWarning(projectCount?: number) {
  if (projectCount === undefined) {
    return <p>{translate('settings.almintegration.delete.no_info')}</p>;
  }

  return projectCount ? (
    <p>{translateWithParameters('settings.almintegration.delete.info', projectCount)} </p>
  ) : null;
}

export default function DeleteModal({ id, onDelete, onCancel, projectCount }: DeleteModalProps) {
  return (
    <ConfirmModal
      confirmButtonText={translate('delete')}
      confirmData={id}
      header={translate('settings.almintegration.delete.header')}
      isDestructive
      onClose={onCancel}
      onConfirm={onDelete}
    >
      <>
        <p className="spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('settings.almintegration.delete.message')}
            id="settings.almintegration.delete.message"
            values={{ id: <b>{id}</b> }}
          />
        </p>
        {showProjectCountWarning(projectCount)}
      </>
    </ConfirmModal>
  );
}
