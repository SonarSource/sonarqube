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
import { FormattedMessage } from 'react-intl';
import {
  DeleteButton,
  ResetButtonLink,
  SubmitButton
} from 'sonar-ui-common/components/controls/buttons';
import SimpleModal, { ChildrenProps } from 'sonar-ui-common/components/controls/SimpleModal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { removeUser } from '../../../api/quality-profiles';
import Avatar from '../../../components/ui/Avatar';

interface Props {
  onDelete: (user: T.UserSelected) => void;
  organization?: string;
  profile: { language: string; name: string };
  user: T.UserSelected;
}

interface State {
  deleteModal: boolean;
}

export default class ProfilePermissionsUser extends React.PureComponent<Props, State> {
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
    const { organization, profile, user } = this.props;

    return removeUser({
      language: profile.language,
      login: user.login,
      organization,
      qualityProfile: profile.name
    }).then(() => {
      this.handleDeleteModalClose();
      this.props.onDelete(user);
    });
  };

  renderDeleteModal = (props: ChildrenProps) => (
    <div>
      <header className="modal-head">
        <h2>{translate('users.remove')}</h2>
      </header>

      <div className="modal-body">
        <FormattedMessage
          defaultMessage={translate('users.remove.confirmation')}
          id="users.remove.confirmation"
          values={{
            user: <strong>{this.props.user.name}</strong>
          }}
        />
      </div>

      <footer className="modal-foot">
        {props.submitting && <i className="spinner spacer-right" />}
        <SubmitButton
          className="button-red"
          disabled={props.submitting}
          onClick={props.onSubmitClick}>
          {translate('remove')}
        </SubmitButton>
        <ResetButtonLink onClick={props.onCloseClick}>{translate('cancel')}</ResetButtonLink>
      </footer>
    </div>
  );

  render() {
    const { user } = this.props;

    return (
      <div className="clearfix big-spacer-bottom">
        <DeleteButton
          className="pull-right spacer-top spacer-left spacer-right button-small"
          onClick={this.handleDeleteClick}
        />
        <Avatar className="pull-left spacer-right" hash={user.avatar} name={user.name} size={32} />
        <div className="overflow-hidden">
          <strong>{user.name}</strong>
          <div className="note">{user.login}</div>
        </div>

        {this.state.deleteModal && (
          <SimpleModal
            header={translate('users.remove')}
            onClose={this.handleDeleteModalClose}
            onSubmit={this.handleDelete}>
            {this.renderDeleteModal}
          </SimpleModal>
        )}
      </div>
    );
  }
}
