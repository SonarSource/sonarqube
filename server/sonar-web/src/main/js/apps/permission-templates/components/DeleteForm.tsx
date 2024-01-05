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
import { DangerButtonPrimary, Modal, Spinner } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { PermissionTemplate } from '../../../types/types';

interface Props {
  onClose: () => void;
  onSubmit: () => Promise<void>;
  permissionTemplate: PermissionTemplate;
}

export default function DeleteForm({ onClose, onSubmit, permissionTemplate: t }: Props) {
  const [submitting, setSubmitting] = useState(false);
  const header = translate('permission_template.delete_confirm_title');

  const handleClick = React.useCallback(() => {
    setSubmitting(true);
    onSubmit();
  }, [onSubmit]);

  return (
    <Modal
      onClose={onClose}
      headerTitle={header}
      secondaryButtonLabel={translate('cancel')}
      body={translateWithParameters(
        'permission_template.do_you_want_to_delete_template_xxx',
        t.name,
      )}
      primaryButton={
        <>
          <Spinner loading={submitting} />
          <DangerButtonPrimary onClick={handleClick} disabled={submitting}>
            {translate('delete')}
          </DangerButtonPrimary>
        </>
      }
    />
  );
}
