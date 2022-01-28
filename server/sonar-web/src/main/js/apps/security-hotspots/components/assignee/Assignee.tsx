/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { assignSecurityHotspot } from '../../../../api/security-hotspots';
import addGlobalSuccessMessage from '../../../../app/utils/addGlobalSuccessMessage';
import { withCurrentUser } from '../../../../components/hoc/withCurrentUser';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isLoggedIn } from '../../../../helpers/users';
import { Hotspot, HotspotStatus } from '../../../../types/security-hotspots';
import { CurrentUser, UserActive } from '../../../../types/types';
import AssigneeRenderer from './AssigneeRenderer';

interface Props {
  currentUser: CurrentUser;
  hotspot: Hotspot;

  onAssigneeChange: () => void;
}

interface State {
  editing: boolean;
  loading: boolean;
}

export class Assignee extends React.PureComponent<Props, State> {
  mounted = false;
  state = {
    editing: false,
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleEnterEditionMode = () => {
    this.setState({ editing: true });
  };

  handleExitEditionMode = () => {
    this.setState({ editing: false });
  };

  handleAssign = (newAssignee: UserActive) => {
    this.setState({ loading: true });
    assignSecurityHotspot(this.props.hotspot.key, {
      assignee: newAssignee?.login
    })
      .then(() => {
        if (this.mounted) {
          this.setState({ editing: false, loading: false });
          this.props.onAssigneeChange();
        }
      })
      .then(() =>
        addGlobalSuccessMessage(
          newAssignee.login
            ? translateWithParameters('hotspots.assign.success', newAssignee.name)
            : translate('hotspots.assign.unassign.success')
        )
      )
      .catch(() => this.setState({ loading: false }));
  };

  render() {
    const {
      currentUser,
      hotspot: { assigneeUser, status }
    } = this.props;
    const { editing, loading } = this.state;

    const canEdit = status === HotspotStatus.TO_REVIEW;

    return (
      <AssigneeRenderer
        assignee={assigneeUser}
        canEdit={canEdit}
        editing={editing}
        loading={loading}
        loggedInUser={isLoggedIn(currentUser) ? currentUser : undefined}
        onAssign={this.handleAssign}
        onEnterEditionMode={this.handleEnterEditionMode}
        onExitEditionMode={this.handleExitEditionMode}
      />
    );
  }
}

export default withCurrentUser(Assignee);
