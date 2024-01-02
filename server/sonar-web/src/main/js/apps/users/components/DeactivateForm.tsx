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
import { deactivateUser } from '../../../api/users';
import DocLink from '../../../components/common/DocLink';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import Modal from '../../../components/controls/Modal';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { UserActive } from '../../../types/users';

export interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: UserActive;
}

interface State {
  submitting: boolean;
  anonymize: boolean;
}

export default class DeactivateForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { submitting: false, anonymize: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleAnonymize = (checked: boolean) => {
    this.setState({ anonymize: checked });
  };

  handleDeactivate = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    deactivateUser({ login: this.props.user.login, anonymize: this.state.anonymize }).then(
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
    const { submitting, anonymize } = this.state;

    const header = translate('users.deactivate_user');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form autoComplete="off" id="deactivate-user-form" onSubmit={this.handleDeactivate}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>
          <div className="modal-body display-flex-column">
            {translateWithParameters('users.deactivate_user.confirmation', user.name, user.login)}
            <Checkbox
              id="delete-user"
              className="big-spacer-top"
              checked={anonymize}
              onCheck={this.handleAnonymize}
            >
              <label className="little-spacer-left" htmlFor="delete-user">
                {translate('users.delete_user')}
              </label>
            </Checkbox>
            {anonymize && (
              <Alert variant="warning" className="big-spacer-top">
                <FormattedMessage
                  defaultMessage={translate('users.delete_user.help')}
                  id="delete-user-warning"
                  values={{
                    link: (
                      <DocLink to="/instance-administration/authentication/overview/">
                        {translate('users.delete_user.help.link')}
                      </DocLink>
                    ),
                  }}
                />
              </Alert>
            )}
          </div>
          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton className="js-confirm button-red" disabled={submitting}>
              {translate('users.deactivate')}
            </SubmitButton>
            <ResetButtonLink className="js-modal-close" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
