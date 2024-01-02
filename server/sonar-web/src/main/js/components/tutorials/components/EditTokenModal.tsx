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
import { generateToken, getTokens, revokeToken } from '../../../api/user-tokens';
import { Button, DeleteButton } from '../../../components/controls/buttons';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import SimpleModal from '../../../components/controls/SimpleModal';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import {
  computeTokenExpirationDate,
  EXPIRATION_OPTIONS,
  getAvailableExpirationOptions,
} from '../../../helpers/tokens';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { TokenExpiration, TokenType } from '../../../types/token';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import Link from '../../common/Link';
import Select from '../../controls/Select';
import { getUniqueTokenName } from '../utils';
import ProjectTokenScopeInfo from './ProjectTokenScopeInfo';

interface State {
  loading: boolean;
  token?: string;
  tokenName: string;
  tokenExpiration: TokenExpiration;
  tokenExpirationOptions: { value: TokenExpiration; label: string }[];
}

interface Props {
  component: Component;
  currentUser: LoggedInUser;
  onClose: (token?: string) => void;
  preferredTokenType?: TokenType.Global | TokenType.Project;
}

export default class EditTokenModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    tokenName: '',
    tokenExpiration: TokenExpiration.OneMonth,
    tokenExpirationOptions: EXPIRATION_OPTIONS,
  };

  componentDidMount() {
    this.mounted = true;
    this.getTokensAndName();
    this.getTokenExpirationOptions();
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
        tokenName: getUniqueTokenName(tokens, `Analyze "${component.name}"`),
      });
    }
  };

  getTokenExpirationOptions = async () => {
    const tokenExpirationOptions = await getAvailableExpirationOptions();
    if (tokenExpirationOptions && this.mounted) {
      this.setState({ tokenExpirationOptions });
    }
  };

  getNewToken = async () => {
    const {
      component: { key },
    } = this.props;
    const { tokenName, tokenExpiration } = this.state;

    const type = this.getTokenType();

    const { token } = await generateToken({
      name: tokenName,
      type,
      projectKey: key,
      ...(tokenExpiration !== TokenExpiration.NoExpiration && {
        expirationDate: computeTokenExpirationDate(tokenExpiration),
      }),
    });

    if (this.mounted) {
      this.setState({
        token,
        tokenName,
      });
    }
  };

  getTokenType = () => {
    const { currentUser, preferredTokenType } = this.props;

    return preferredTokenType === TokenType.Global &&
      hasGlobalPermission(currentUser, Permissions.Scan)
      ? TokenType.Global
      : TokenType.Project;
  };

  handleTokenNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({
      tokenName: event.target.value,
    });
  };

  handleTokenExpirationChange = ({ value }: { value: TokenExpiration }) => {
    this.setState({ tokenExpiration: value });
  };

  handleTokenRevoke = async () => {
    const { tokenName } = this.state;

    if (tokenName) {
      await revokeToken({ name: tokenName });

      if (this.mounted) {
        this.setState({
          token: '',
          tokenName: '',
        });
      }
    }
  };

  render() {
    const { loading, token, tokenName, tokenExpiration, tokenExpirationOptions } = this.state;

    const type = this.getTokenType();
    const header = translate('onboarding.token.generate', type);
    const intro = translate('onboarding.token.text', type);

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
                  defaultMessage={intro}
                  id={intro}
                  values={{
                    link: (
                      <Link target="_blank" to="/account/security">
                        {translate('onboarding.token.text.user_account')}
                      </Link>
                    ),
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

                    <DeleteButton
                      aria-label={translate('users.tokens.revoke_token')}
                      onClick={this.handleTokenRevoke}
                    />
                  </div>

                  <Alert className="big-spacer-top" variant="warning">
                    {translateWithParameters('users.tokens.new_token_created', token)}
                  </Alert>
                </>
              ) : (
                <>
                  <div className="big-spacer-top display-flex-center">
                    {loading ? (
                      <DeferredSpinner />
                    ) : (
                      <>
                        <div className="display-flex-column">
                          <label className="text-bold little-spacer-bottom" htmlFor="token-name">
                            {translate('onboarding.token.name.label')}
                          </label>
                          <input
                            className="input-large spacer-right text-middle"
                            onChange={this.handleTokenNameChange}
                            required={true}
                            id="token-name"
                            type="text"
                            placeholder={translate('onboarding.token.name.placeholder')}
                            value={tokenName}
                          />
                        </div>
                        <div className="display-flex-column">
                          <label
                            className="text-bold little-spacer-bottom"
                            htmlFor="token-expiration"
                          >
                            {translate('users.tokens.expires_in')}
                          </label>
                          <div className="display-flex-center">
                            <Select
                              id="token-expiration"
                              className="abs-width-100 spacer-right"
                              isSearchable={false}
                              onChange={this.handleTokenExpirationChange}
                              options={tokenExpirationOptions}
                              value={tokenExpirationOptions.find(
                                (option) => option.value === tokenExpiration
                              )}
                            />
                            <Button
                              className="text-middle"
                              disabled={!tokenName}
                              onClick={this.getNewToken}
                            >
                              {translate('onboarding.token.generate')}
                            </Button>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                  {type === TokenType.Project && <ProjectTokenScopeInfo />}
                </>
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
