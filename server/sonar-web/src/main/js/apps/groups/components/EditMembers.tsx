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
import * as React from 'react';
import { ButtonIcon } from '../../../components/controls/buttons';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import { translate } from '../../../helpers/l10n';
import { Group } from '../../../types/types';
import EditMembersModal from './EditMembersModal';

interface Props {
  group: Group;
  onEdit: () => void;
}

interface State {
  modal: boolean;
}

export default class EditMembers extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
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
    if (this.mounted) {
      this.setState({ modal: false });
      this.props.onEdit();
    }
  };

  render() {
    return (
      <>
        <ButtonIcon
          aria-label={translate('groups.users.edit')}
          className="button-small"
          onClick={this.handleMembersClick}
          title={translate('groups.users.edit')}
        >
          <BulletListIcon />
        </ButtonIcon>
        {this.state.modal && (
          <EditMembersModal group={this.props.group} onClose={this.handleModalClose} />
        )}
      </>
    );
  }
}
