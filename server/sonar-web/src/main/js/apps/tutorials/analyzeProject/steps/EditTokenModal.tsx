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
import { Link } from 'react-router';
import { Button, DeleteButton } from 'sonar-ui-common/components/controls/buttons';
import ConfirmModal from 'sonar-ui-common/components/controls/ConfirmModal';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { generateToken, getTokens, revokeToken } from '../../../../api/user-tokens';
import { RenderOptions } from '../../components/RenderOptions';
import { getUniqueTokenName } from '../../utils';

export enum TokenMode {
  use_existing_token = 'use_existing_token',
  generate_token = 'generate_token'
}

interface State {
  existingToken?: string;
  mode: TokenMode;
  token?: string;
  tokenName: string;
}

interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
  onClose: VoidFunction;
  onSave: (token: string) => void;
}

export default class EditTokenModal extends React.PureComponent<Props, State> {
  mounted = false;
  initialTokenName = '';
  state = {
    mode: TokenMode.use_existing_token,
    existingToken: '',
    token: '',
    tokenName: ''
  };

  componentDidMount() {
    this.mounted = true;
    const { component } = this.props;
    this.initialTokenName = `Analyze "${component.name}"`;
    this.getTokensAndName();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getTokensAndName = () => {
    const { currentUser } = this.props;

    getTokens(currentUser.login).then(
      t => {
        if (this.mounted) {
          this.setState({ tokenName: getUniqueTokenName(t, this.initialTokenName) });
        }
      },
      () => {}
    );
  };

  getNewToken = () => {
    const { tokenName = this.initialTokenName } = this.state;

    generateToken({ name: tokenName }).then(
      ({ token }: { token: string }) => {
        if (this.mounted) {
          this.setState({
            token,
            tokenName
          });
        }
      },
      () => {}
    );
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (this.mounted) {
      this.setState({
        tokenName: event.target.value
      });
    }
  };

  handleTokenRevoke = () => {
    const { tokenName } = this.state;

    if (tokenName) {
      revokeToken({ name: tokenName }).then(
        () => {
          if (this.mounted) {
            this.setState({
              mode: TokenMode.use_existing_token,
              token: '',
              tokenName: ''
            });
          }
        },
        () => {}
      );
    }
  };

  setExistingToken = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (this.mounted) {
      this.setState({
        existingToken: event.target.value
      });
    }
  };

  setMode = (mode: TokenMode) => {
    this.setState({ mode });
  };

  render() {
    const { onClose, onSave } = this.props;
    const { existingToken, mode, token, tokenName } = this.state;

    const header = translate('onboarding.token.header');

    const isConfirmEnabled =
      (mode === TokenMode.generate_token && token) ||
      (mode === TokenMode.use_existing_token && existingToken);

    const onConfirm = () => {
      if (mode === TokenMode.generate_token && token) {
        onSave(token);
      } else if (mode === TokenMode.use_existing_token && existingToken) {
        onSave(existingToken);
      }
    };

    return (
      <ConfirmModal
        confirmButtonText={translate('save')}
        confirmDisable={!isConfirmEnabled}
        header={header}
        onClose={onClose}
        onConfirm={onConfirm}>
        <p className="spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('onboarding.token.text')}
            id="onboarding.token.text"
            values={{
              link: (
                <Link target="_blank" to="/account/security">
                  {translate('onboarding.token.text.user_account')}
                </Link>
              )
            }}
          />
        </p>

        {token ? (
          <>
            <span className="text-middle">
              {tokenName}
              {': '}
            </span>
            <strong className="spacer-right text-middle">{token}</strong>

            <DeleteButton className="button-small text-middle" onClick={this.handleTokenRevoke} />

            <Alert className="big-spacer-top" variant="warning">
              {translateWithParameters('users.tokens.new_token_created', token)}
            </Alert>
          </>
        ) : (
          <>
            <RenderOptions
              checked={mode}
              name="token-mode"
              onCheck={this.setMode}
              optionLabelKey="onboarding.token"
              options={[TokenMode.use_existing_token, TokenMode.generate_token]}
            />

            <div className="big-spacer-top">
              {mode === TokenMode.generate_token && (
                <>
                  <input
                    className="input-super-large spacer-right text-middle"
                    onChange={this.handleChange}
                    placeholder={translate('onboarding.token.generate_token.placeholder')}
                    required={true}
                    type="text"
                    value={tokenName}
                  />
                  <Button className="text-middle" disabled={!tokenName} onClick={this.getNewToken}>
                    {translate('onboarding.token.generate')}
                  </Button>
                </>
              )}

              {mode === TokenMode.use_existing_token && (
                <input
                  className="input-super-large spacer-right text-middle"
                  onChange={this.setExistingToken}
                  placeholder={translate('onboarding.token.use_existing_token.placeholder')}
                  required={true}
                  type="text"
                  value={existingToken}
                />
              )}
            </div>
          </>
        )}
      </ConfirmModal>
    );
  }
}
