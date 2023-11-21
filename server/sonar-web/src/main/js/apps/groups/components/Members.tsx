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
import * as React from 'react';
import { ButtonIcon } from '../../../components/controls/buttons';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import EditMembersModal from './EditMembersModal';
import ViewMembersModal from './ViewMembersModal';

interface Props {
  isManaged: boolean;
  group: Group;
  onEdit: () => void;
}

export default function Members(props: Readonly<Props>) {
  const [openModal, setOpenModal] = React.useState(false);
  const { isManaged, group } = props;

  const handleModalClose = () => {
    setOpenModal(false);
    if (!isManaged && !group.default) {
      props.onEdit();
    }
  };

  return (
    <>
      <ButtonIcon
        aria-label={translateWithParameters(
          isManaged || group.default ? 'groups.users.view' : 'groups.users.edit',
          group.name,
        )}
        className="button-small little-spacer-left little-padded"
        onClick={() => setOpenModal(true)}
        title={translateWithParameters('groups.users.edit', group.name)}
      >
        <BulletListIcon />
      </ButtonIcon>
      {openModal &&
        (isManaged || group.default ? (
          <ViewMembersModal isManaged={isManaged} group={group} onClose={handleModalClose} />
        ) : (
          <EditMembersModal group={group} onClose={handleModalClose} />
        ))}
    </>
  );
}
