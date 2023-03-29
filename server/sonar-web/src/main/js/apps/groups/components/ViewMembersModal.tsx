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
import { DeferredSpinner } from 'design-system/lib';
import * as React from 'react';
import { getUsersInGroup } from '../../../api/user_groups';
import { ResetButtonLink } from '../../../components/controls/buttons';
import ListFooter from '../../../components/controls/ListFooter';
import Modal from '../../../components/controls/Modal';
import SearchBox from '../../../components/controls/SearchBox';
import { SelectListFilter } from '../../../components/controls/SelectList';
import { translate } from '../../../helpers/l10n';
import { Group, UserGroupMember } from '../../../types/types';

interface Props {
  isManaged: boolean;
  group: Group;
  onClose: () => void;
}

export default function ViewMembersModal(props: Props) {
  const { isManaged, group } = props;

  const [loading, setLoading] = React.useState(false);
  const [page, setPage] = React.useState(1);
  const [query, setQuery] = React.useState<string>();
  const [total, setTotal] = React.useState<number>();
  const [users, setUsers] = React.useState<UserGroupMember[]>([]);

  React.useEffect(() => {
    (async () => {
      setLoading(true);
      const data = await getUsersInGroup({
        name: group.name,
        p: page,
        q: query,
        selected: SelectListFilter.Selected,
      });
      if (page > 1) {
        setUsers([...users, ...data.users]);
      } else {
        setUsers(data.users);
      }
      setTotal(data.total);
      setLoading(false);
    })();
  }, [query, page]);

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
          loading={loading}
          onChange={(q) => {
            setQuery(q);
            setPage(1);
          }}
          placeholder={translate('search_verb')}
          value={query}
        />
        <div className="select-list-list-container spacer-top">
          <DeferredSpinner loading={loading}>
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
          </DeferredSpinner>
        </div>
        {total !== undefined && (
          <ListFooter count={users.length} loadMore={() => setPage((p) => p + 1)} total={total} />
        )}
      </div>

      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}
