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
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  IconMoreVertical,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { RESTORE, DELETE } from '../constants';
import { OrganizationDelete } from './OrganizationDelete';
import { OrganizationRestore } from './OrganizationRestore';

interface Props {
  organization: ArchivedOrganization;
}

export default function Actions(props: Props) {
  const { organization } = props;
  const [openPopup, setOpenPopup] = React.useState<string | undefined>(undefined);

  const closePopup = ()=>{
    setOpenPopup(undefined);
  };

  return (
    <>
      <DropdownMenu.Root
        items={
          <>
          {organization.actions[RESTORE] &&
            <DropdownMenu.ItemButton
              className="org-restore"
              key="restore"
              onClick={() => setOpenPopup('restore')}
            >
              {translate('restore')}
            </DropdownMenu.ItemButton>
          }
          {organization.actions[DELETE] &&
            <DropdownMenu.ItemButton
              className="org-delete"
              key="delete"
              onClick={() => setOpenPopup('delete')}
            >
              {translate('delete')}
            </DropdownMenu.ItemButton>
          }
          </>
        }
      >
        <ButtonIcon
          id={`org-settings-action-dropdown-${organization.kee}`}
          className="org-actions-toggle"
          Icon={IconMoreVertical}
          variety={ButtonVariety.DefaultGhost}
        />
      </DropdownMenu.Root>

      { openPopup === 'restore' &&
        <OrganizationRestore organization={organization} onClose={closePopup} />
      }
      { openPopup === 'delete' &&
        <OrganizationDelete organization={organization} onClose={closePopup} />
      }

    </>
  );
}
