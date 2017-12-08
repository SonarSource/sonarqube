/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import TokensFormItem from './TokensFormItem';
import TokensFormNewToken from './TokensFormNewToken';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { User } from '../../../api/users';
import { getTokens, generateToken, UserToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';

interface Props {
  user: User;
  onClose: () => void;
  onUpdateUsers: () => void;
}

interface State {
  generating: boolean;
  hasChanged: boolean;
  loading: boolean;
  newToken?: { name: string; token: string };
  newTokenName: string;
  tokens: UserToken[];
}

export default class TokensForm extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = {
    generating: false,
    hasChanged: false,
    loading: true,
    newTokenName: '',
    tokens: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchTokens();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchTokens = ({ user } = this.props) => {
    this.setState({ loading: true });
    getTokens(user.login).then(
      tokens => {
        if (this.mounted) {
          this.setState({ loading: false, tokens });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleCloseClick = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.preventDefault();
    this.handleClose();
  };

  handleClose = () => {
    if (this.state.hasChanged) {
      this.props.onUpdateUsers();
    }
    this.props.onClose();
  };

  handleGenerateToken = (evt: React.SyntheticEvent<HTMLFormElement>) => {
    evt.preventDefault();
    if (this.state.newTokenName.length > 0) {
      this.setState({ generating: true });
      generateToken({ name: this.state.newTokenName, login: this.props.user.login }).then(
        newToken => {
          if (this.mounted) {
            this.fetchTokens();
            this.setState({ generating: false, hasChanged: true, newToken, newTokenName: '' });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ generating: false });
          }
        }
      );
    }
  };

  handleRevokeToken = () => {
    this.setState({ hasChanged: true });
    this.fetchTokens();
  };

  handleNewTokenChange = (evt: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ newTokenName: evt.currentTarget.value });

  renderItems() {
    const { tokens } = this.state;
    if (tokens.length <= 0) {
      return (
        <tr>
          <td colSpan={3} className="note">
            {translate('users.no_tokens')}
          </td>
        </tr>
      );
    }
    return tokens.map(token => (
      <TokensFormItem
        key={token.name}
        token={token}
        onRevokeToken={this.handleRevokeToken}
        user={this.props.user}
      />
    ));
  }

  render() {
    const { generating, loading, newToken, newTokenName, tokens } = this.state;
    const header = translate('users.tokens');
    const customSpinner = (
      <tr>
        <td>
          <i className="spinner" />
        </td>
      </tr>
    );
    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body modal-container">
          <h3 className="spacer-bottom">{translate('users.generate_tokens')}</h3>
          <form id="generate-token-form" onSubmit={this.handleGenerateToken} autoComplete="off">
            <input
              className="spacer-right"
              type="text"
              maxLength={100}
              onChange={this.handleNewTokenChange}
              placeholder={translate('users.enter_token_name')}
              required={true}
              value={newTokenName}
            />
            <button
              className="js-generate-token"
              disabled={generating || newTokenName.length <= 0}
              type="submit">
              {translate('users.generate')}
            </button>
          </form>

          {newToken && <TokensFormNewToken token={newToken} />}

          <table className="data zebra big-spacer-top ">
            <thead>
              <tr>
                <th>{translate('name')}</th>
                <th className="text-right">{translate('created')}</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <DeferredSpinner
                customSpinner={customSpinner}
                loading={loading && tokens.length <= 0}>
                {this.renderItems()}
              </DeferredSpinner>
            </tbody>
          </table>
        </div>
        <footer className="modal-foot">
          <a className="js-modal-close" href="#" onClick={this.handleCloseClick}>
            {translate('Done')}
          </a>
        </footer>
      </Modal>
    );
  }
}
