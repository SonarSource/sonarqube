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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { generateToken, getTokens, revokeToken } from '../../../api/user-tokens';
import { Button, DeleteButton } from '../../../components/controls/buttons';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import SimpleModal from '../../../components/controls/SimpleModal';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Component, LoggedInUser } from '../../../types/types';
import { getUniqueTokenName } from '../utils';

interface State {
  loading: boolean;
  token?: string;
  tokenName: string;
}

interface Props {
  component: Component;
  currentUser: LoggedInUser;
  onClose: (token?: string) => void;
}

export default class EditTokenModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    tokenName: ''
  };

  componentDidMount() {
    this.mounted = true;
    this.getTokensAndName();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getTokensAndName = async () => {
    const { component, currentUser } = this.props;

    const tokens = await getTokens(currentUser.login);

    if (this.mounted) {
      this.setState({
        loading: false,
        tokenName: getUniqueTokenName(tokens, `Analyze "${component.name}"`)
      });
    }
  };

  getNewToken = async () => {
    const { tokenName } = this.state;

    const { token } = await generateToken({ name: tokenName });

    if (this.mounted) {
      this.setState({
        token,
        tokenName
      });
    }
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({
      tokenName: event.target.value
    });
  };

  handleTokenRevoke = async () => {
    const { tokenName } = this.state;

    if (tokenName) {
      await revokeToken({ name: tokenName });

      if (this.mounted) {
        this.setState({
          token: '',
          tokenName: ''
        });
      }
    }
  };

  render() {
    const { loading, token, tokenName } = this.state;

    const header = translate('onboarding.token.generate_token');

    return (
      <SimpleModal header={header} onClose={this.props.onClose} onSubmit={this.props.onClose}>
        {({ onCloseClick }) => (
          <>
            <div className="modal-head">
              <h2>{header}</h2>
            </div>

            <div className="modal-body">
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
                  <div className="display-float-center">
                    <code className="rule spacer-right">{token}</code>

                    <ClipboardIconButton copyValue={token} />

                    <DeleteButton onClick={this.handleTokenRevoke} />
                  </div>

                  <Alert className="big-spacer-top" variant="warning">
                    {translateWithParameters('users.tokens.new_token_created', token)}
                  </Alert>
                </>
              ) : (
                <div className="big-spacer-top">
                  {loading ? (
                    <DeferredSpinner />
                  ) : (
                    <>
                      <input
                        className="input-super-large spacer-right text-middle"
                        onChange={this.handleChange}
                        placeholder={translate('onboarding.token.generate_token.placeholder')}
                        required={true}
                        type="text"
                        value={tokenName}
                      />
                      <Button
                        className="text-middle"
                        disabled={!tokenName}
                        onClick={this.getNewToken}>
                        {translate('onboarding.token.generate')}
                      </Button>
                    </>
                  )}
                </div>
              )}
            </div>
            <div className="modal-foot">
              <Button onClick={onCloseClick}>{translate('continue')}</Button>
            </div>
          </>
        )}
      </SimpleModal>
    );
  }
}
