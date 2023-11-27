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
import { Spinner } from 'design-system';
import * as React from 'react';
import ListFooter from '../../../components/controls/ListFooter';
import Modal from '../../../components/controls/Modal';
import SearchBox from '../../../components/controls/SearchBox';
import { ResetButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { useGroupMembersQuery } from '../../../queries/group-memberships';
import { Group } from '../../../types/types';

interface Props {
  isManaged: boolean;
  group: Group;
  onClose: () => void;
}

export default function ViewMembersModal(props: Readonly<Props>) {
  const { isManaged, group } = props;

  const [query, setQuery] = React.useState<string>();
  const { data, isLoading, fetchNextPage } = useGroupMembersQuery({
    q: query,
    groupId: group.id,
  });

  const users = data?.pages.flatMap((page) => page.users) ?? [];

  const modalHeader = translate('users.list');
  return (
    <Modal
      className="group-menbers-modal"
      contentLabel={modalHeader}
      onRequestClose={props.onClose}
    >
      <header className="modal-head">
        <h2>{modalHeader}</h2>
      </header>

      <div className="modal-body modal-container">
        <SearchBox
          className="view-search-box"
          loading={isLoading}
          onChange={setQuery}
          placeholder={translate('search_verb')}
          value={query}
        />
        <div className="select-list-list-container spacer-top sw-overflow-auto">
          <Spinner loading={isLoading}>
            <ul className="menu">
              {users.map((user) => (
                <li key={user.login} className="display-flex-center">
                  <span className="little-spacer-left width-100">
                    <span className="select-list-list-item display-flex-center display-flex-space-between">
                      <span className="spacer-right">
                        {user.name}
                        <br />
                        <span className="note">{user.login}</span>
                      </span>
                      {!user.managed && isManaged && (
                        <span className="badge">{translate('local')}</span>
                      )}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </Spinner>
        </div>
        {data !== undefined && (
          <ListFooter
            count={users.length}
            loadMore={fetchNextPage}
            total={data?.pages[0].page.total}
          />
        )}
      </div>

      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}
