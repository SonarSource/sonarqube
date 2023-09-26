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
import { Avatar, DestructiveIcon, Note, TrashIcon } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { removeUser } from '../../../api/quality-profiles';
import SimpleModal, { ChildrenProps } from '../../../components/controls/SimpleModal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';

interface Props {
  onDelete: (user: UserSelected) => void;
  profile: { language: string; name: string };
  user: UserSelected;
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
    const { profile, user } = this.props;

    return removeUser({
      language: profile.language,
      login: user.login,
      qualityProfile: profile.name,
    }).then(() => {
      this.handleDeleteModalClose();
      this.props.onDelete(user);
    });
  };

  renderDeleteModal = (props: ChildrenProps) => (
    <div>
      <header className="modal-head">
        <h2>{translate('quality_profiles.permissions.remove.user')}</h2>
      </header>

      <div className="modal-body">
        <FormattedMessage
          defaultMessage={translate('quality_profiles.permissions.remove.user.confirmation')}
          id="quality_profiles.permissions.remove.user.confirmation"
          values={{
            user: <strong>{this.props.user.name}</strong>,
          }}
        />
      </div>

      <footer className="modal-foot">
        {props.submitting && <i className="spinner spacer-right" />}
        <SubmitButton
          className="button-red"
          disabled={props.submitting}
          onClick={props.onSubmitClick}
        >
          {translate('remove')}
        </SubmitButton>
        <ResetButtonLink onClick={props.onCloseClick}>{translate('cancel')}</ResetButtonLink>
      </footer>
    </div>
  );

  render() {
    const { user } = this.props;

    return (
      <div className="sw-flex sw-items-center sw-justify-between">
        <div className="sw-flex sw-truncate">
          <Avatar
            className="sw-mt-1/2 sw-mr-3 sw-grow-0 sw-shrink-0"
            hash={user.avatar}
            name={user.name}
            size="xs"
          />
          <div className="sw-truncate fs-mask">
            <strong className="sw-body-sm-highlight">{user.name}</strong>
            <Note className="sw-block">{user.login}</Note>
          </div>
        </div>
        <DestructiveIcon
          Icon={TrashIcon}
          aria-label={translateWithParameters(
            'quality_profiles.permissions.remove.user_x',
            user.name,
          )}
          onClick={this.handleDeleteClick}
        />

        {this.state.deleteModal && (
          <SimpleModal
            header={translate('quality_profiles.permissions.remove.user')}
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
