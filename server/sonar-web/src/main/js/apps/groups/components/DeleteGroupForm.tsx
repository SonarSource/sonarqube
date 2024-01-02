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
import { DangerButtonPrimary, Modal } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useDeleteGroupMutation } from '../../../queries/groups';
import { Group } from '../../../types/types';

interface Props {
  group: Group;
  onClose: () => void;
}

export default function DeleteGroupForm(props: Readonly<Props>) {
  const { group } = props;

  const { mutate: deleteGroup, isLoading } = useDeleteGroupMutation();

  const onSubmit = () => {
    deleteGroup(group.id, {
      onSuccess: props.onClose,
    });
  };

  return (
    <Modal
      headerTitle={translate('groups.delete_group')}
      onClose={props.onClose}
      body={translateWithParameters('groups.delete_group.confirmation', group.name)}
      primaryButton={
        <DangerButtonPrimary autoFocus type="submit" onClick={onSubmit} disabled={isLoading}>
          {translate('delete')}
        </DangerButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
