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

interface State {
  modal: boolean;
}

export default class Members extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleMembersClick = () => {
    this.setState({ modal: true });
  };

  handleModalClose = () => {
    const { isManaged, group } = this.props;
    if (this.mounted) {
      this.setState({ modal: false });
      if (!isManaged && !group.default) {
        this.props.onEdit();
      }
    }
  };

  render() {
    const { isManaged, group } = this.props;
    return (
      <>
        <ButtonIcon
          aria-label={translateWithParameters(
            isManaged || group.default ? 'groups.users.view' : 'groups.users.edit',
            group.name,
          )}
          className="button-small little-spacer-left little-padded"
          onClick={this.handleMembersClick}
          title={translateWithParameters('groups.users.edit', group.name)}
        >
          <BulletListIcon />
        </ButtonIcon>
        {this.state.modal &&
          (isManaged || group.default ? (
            <ViewMembersModal isManaged={isManaged} group={group} onClose={this.handleModalClose} />
          ) : (
            <EditMembersModal group={group} onClose={this.handleModalClose} />
          ))}
      </>
    );
  }
}
