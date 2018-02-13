/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Modal from '../../../components/controls/Modal';
import { deactivateUser } from '../../../api/users';
import { User } from '../../../app/types';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: User;
}

interface State {
  submitting: boolean;
}

export default class DeactivateForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { submitting: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleDeactivate = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    deactivateUser({ login: this.props.user.login }).then(
      () => {
        this.props.onUpdateUsers();
        this.props.onClose();
      },
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      }
    );
  };

  render() {
    const { user } = this.props;
    const { submitting } = this.state;

    const header = translate('users.deactivate_user');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="deactivate-user-form" onSubmit={this.handleDeactivate} autoComplete="off">
          <header className="modal-head">
            <h2>{header}</h2>
          </header>
          <div className="modal-body">
            {translateWithParameters('users.deactivate_user.confirmation', user.name, user.login)}
          </div>
          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <button className="js-confirm button-red" disabled={submitting} type="submit">
              {translate('users.deactivate')}
            </button>
            <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </footer>
        </form>
      </Modal>
    );
  }
}
