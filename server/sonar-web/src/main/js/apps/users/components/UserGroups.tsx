/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import GroupsForm from './GroupsForm';
import BulletListIcon from '../../../components/icons-components/BulletListIcon';
import { ButtonIcon } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  groups: string[];
  onUpdateUsers: () => void;
  user: T.User;
}

interface State {
  openForm: boolean;
  showMore: boolean;
}

const GROUPS_LIMIT = 3;

export default class UserGroups extends React.PureComponent<Props, State> {
  state: State = { openForm: false, showMore: false };

  handleOpenForm = () => this.setState({ openForm: true });
  handleCloseForm = () => this.setState({ openForm: false });

  toggleShowMore = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.preventDefault();
    this.setState(state => ({ showMore: !state.showMore }));
  };

  render() {
    const { groups } = this.props;
    const limit = groups.length > GROUPS_LIMIT ? GROUPS_LIMIT - 1 : GROUPS_LIMIT;
    return (
      <ul>
        {groups.slice(0, limit).map(group => (
          <li className="little-spacer-bottom" key={group}>
            {group}
          </li>
        ))}
        {groups.length > GROUPS_LIMIT &&
          this.state.showMore &&
          groups.slice(limit).map(group => (
            <li className="little-spacer-bottom" key={group}>
              {group}
            </li>
          ))}
        <li className="little-spacer-bottom">
          {groups.length > GROUPS_LIMIT && !this.state.showMore && (
            <a className="js-user-more-groups spacer-right" href="#" onClick={this.toggleShowMore}>
              {translateWithParameters('more_x', groups.length - limit)}
            </a>
          )}
          <ButtonIcon
            className="js-user-groups button-small"
            onClick={this.handleOpenForm}
            tooltip={translate('users.update_groups')}>
            <BulletListIcon />
          </ButtonIcon>
        </li>
        {this.state.openForm && (
          <GroupsForm
            onClose={this.handleCloseForm}
            onUpdateUsers={this.props.onUpdateUsers}
            user={this.props.user}
          />
        )}
      </ul>
    );
  }
}
