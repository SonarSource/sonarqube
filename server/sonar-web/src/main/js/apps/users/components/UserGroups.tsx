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
import { ButtonIcon, ButtonLink } from '../../../components/controls/buttons';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { User } from '../../../types/users';
import GroupsForm from './GroupsForm';

interface Props {
  groups: string[];
  user: User;
  manageProvider: string | undefined;
}

const GROUPS_LIMIT = 3;

export default function UserGroups(props: Props) {
  const { groups, user, manageProvider } = props;

  const [openForm, setOpenForm] = React.useState(false);
  const [showMore, setShowMore] = React.useState(false);

  const limit = groups.length > GROUPS_LIMIT ? GROUPS_LIMIT - 1 : GROUPS_LIMIT;
  return (
    <ul>
      {groups.slice(0, limit).map((group) => (
        <li className="little-spacer-bottom" key={group}>
          {group}
        </li>
      ))}
      {groups.length > GROUPS_LIMIT &&
        showMore &&
        groups.slice(limit).map((group) => (
          <li className="little-spacer-bottom" key={group}>
            {group}
          </li>
        ))}
      <li className="little-spacer-bottom">
        {groups.length > GROUPS_LIMIT && !showMore && (
          <ButtonLink
            className="js-user-more-groups spacer-right"
            onClick={() => setShowMore(!showMore)}
          >
            {translateWithParameters('more_x', groups.length - limit)}
          </ButtonLink>
        )}
        {manageProvider === undefined && (
          <ButtonIcon
            aria-label={translateWithParameters('users.update_users_groups', user.login)}
            className="js-user-groups button-small"
            onClick={() => setOpenForm(true)}
            tooltip={translate('users.update_groups')}
          >
            <BulletListIcon />
          </ButtonIcon>
        )}
      </li>
      {openForm && <GroupsForm onClose={() => setOpenForm(false)} user={user} />}
    </ul>
  );
}
