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
import { FormattedMessage } from 'react-intl';
import { removeGroup } from '../../../api/quality-profiles';
import { Button, DeleteButton, ResetButtonLink } from '../../../components/controls/buttons';
import SimpleModal, { ChildrenProps } from '../../../components/controls/SimpleModal';
import GroupIcon from '../../../components/icons/GroupIcon';
import { translate } from '../../../helpers/l10n';
import { Group } from './ProfilePermissions';

interface Props {
  group: Group;
  onDelete: (group: Group) => void;
  profile: { language: string; name: string };
}

interface State {
  deleteModal: boolean;
}

export default class ProfilePermissionsGroup extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { deleteModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDeleteClick = () => {
    this.setState({ deleteModal: true });
  };

  handleDeleteModalClose = () => {
    if (this.mounted) {
      this.setState({ deleteModal: false });
    }
  };

  handleDelete = () => {
    const { group, profile } = this.props;

    return removeGroup({
      group: group.name,
      language: profile.language,
      qualityProfile: profile.name,
    }).then(() => {
      this.handleDeleteModalClose();
      this.props.onDelete(group);
    });
  };

  renderDeleteModal = (props: ChildrenProps) => (
    <div>
      <header className="modal-head">
        <h2>{translate('quality_profiles.permissions.remove.group')}</h2>
      </header>

      <div className="modal-body">
        <FormattedMessage
          defaultMessage={translate('quality_profiles.permissions.remove.group.confirmation')}
          id="quality_profiles.permissions.remove.group.confirmation"
          values={{
            user: <strong>{this.props.group.name}</strong>,
          }}
        />
      </div>

      <footer className="modal-foot">
        {props.submitting && <i className="spinner spacer-right" />}
        <Button className="button-red" disabled={props.submitting} onClick={props.onSubmitClick}>
          {translate('remove')}
        </Button>
        <ResetButtonLink onClick={props.onCloseClick}>{translate('cancel')}</ResetButtonLink>
      </footer>
    </div>
  );

  render() {
    const { group } = this.props;

    return (
      <div className="clearfix big-spacer-bottom">
        <DeleteButton
          className="pull-right spacer-top spacer-left spacer-right button-small"
          onClick={this.handleDeleteClick}
        />
        <GroupIcon className="pull-left spacer-right" size={32} />
        <div className="overflow-hidden" style={{ lineHeight: '32px' }}>
          <strong>{group.name}</strong>
        </div>

        {this.state.deleteModal && (
          <SimpleModal
            header={translate('quality_profiles.permissions.remove.group')}
            onClose={this.handleDeleteModalClose}
            onSubmit={this.handleDelete}
          >
            {this.renderDeleteModal}
          </SimpleModal>
        )}
      </div>
    );
  }
}
