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

import { FlagMessage } from '~design-system';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  component: {
    name: string;
    qualifier: string;
  };
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export default function PublicProjectDisclaimer({ component, isOpen, onClose, onConfirm }: Props) {
  const { qualifier } = component;
  return (
    <ConfirmModal
      confirmButtonText={translate('projects_role.turn_project_to_public', qualifier)}
      header={translateWithParameters('projects_role.turn_x_to_public', component.name)}
      onClose={onClose}
      onConfirm={onConfirm}
      isOpen={isOpen}
    >
      <FlagMessage className="sw-mb-4" variant="warning">
        {translate('projects_role.are_you_sure_to_turn_project_to_public.warning', qualifier)}
      </FlagMessage>
      <p>{translate('projects_role.are_you_sure_to_turn_project_to_public', qualifier)}</p>
    </ConfirmModal>
  );
}
