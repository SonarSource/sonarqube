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
import { Button, Modal, ModalSize, Spinner, Text } from '@sonarsource/echoes-react';
import { Badge, InputSearch } from 'design-system/lib';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { useUserGroupsQuery } from '../../../queries/group-memberships';
import { RestUserDetailed } from '../../../types/users';

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

export default function ViewGroupsModal(props: Readonly<Props>) {
  const { onClose, user } = props;
  const [query, setQuery] = React.useState<string>('');
  const { data: groups, isLoading } = useUserGroupsQuery({
    q: query,
    userId: user.id,
  });

  const modalBody = (
    <>
      <InputSearch
        className="sw-w-full"
        loading={isLoading}
        onChange={setQuery}
        placeholder={translate('search_verb')}
        value={query}
      />
      <div className="sw-mt-6">
        <Spinner isLoading={isLoading} />
        <ul className="sw-flex sw-flex-col sw-gap-4">
          {(groups || []).map(({ id, description, managed, name }) => (
            <li key={id} className="sw-flex sw-items-center">
              <span className="sw-flex sw-gap-2 sw-justify-between sw-items-center sw-w-full">
                <span>
                  {name}
                  <br />
                  <Text isSubdued> {description} </Text>
                </span>
                {!managed && <Badge>{translate('local')}</Badge>}
              </span>
            </li>
          ))}
        </ul>
      </div>
    </>
  );

  return (
    <Modal
      content={modalBody}
      isOpen
      onOpenChange={onClose}
      secondaryButton={<Button onClick={onClose}>{translate('close')}</Button>}
      size={ModalSize.Default}
      title={translate('users.view_groups')}
    />
  );
}
