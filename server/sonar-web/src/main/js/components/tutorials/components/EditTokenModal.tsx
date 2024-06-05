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
import {
  ButtonSecondary,
  ClipboardIconButton,
  DestructiveIcon,
  FlagMessage,
  InputField,
  InputSelect,
  LabelValueSelectOption,
  Link,
  Modal,
  Spinner,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { generateToken, getTokens, revokeToken } from '../../../api/user-tokens';

import { translate, translateWithParameters } from '../../../helpers/l10n';
import {
  EXPIRATION_OPTIONS,
  computeTokenExpirationDate,
  getAvailableExpirationOptions,
} from '../../../helpers/tokens';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { TokenExpiration, TokenType } from '../../../types/token';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import { getUniqueTokenName } from '../utils';
import { InlineSnippet } from './InlineSnippet';
import ProjectTokenScopeInfo from './ProjectTokenScopeInfo';

interface State {
  loading: boolean;
  token?: string;
  tokenExpiration: TokenExpiration;
  tokenExpirationOptions: { label: string; value: TokenExpiration }[];
  tokenName: string;
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
    this.setState({ tokenName: event.currentTarget.value });
  };

  handleTokenExpirationChange = (value: TokenExpiration) => {
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

  renderForm(type: TokenType) {
    const { loading, token, tokenName, tokenExpiration, tokenExpirationOptions } = this.state;
    const intro = translate('onboarding.token.text', type);

    return (
      <div className="sw-body-sm">
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

        {token ? (
          <>
            <span>
              {tokenName}
              {': '}
            </span>
            <div>
              <InlineSnippet snippet={token} />
              <ClipboardIconButton
                copyLabel={translate('copy_to_clipboard')}
                className="sw-ml-2"
                copyValue={token}
              />
              <DestructiveIcon
                className="sw-ml-1"
                Icon={TrashIcon}
                aria-label={translate('onboarding.token.delete')}
                onClick={this.handleTokenRevoke}
              />
            </div>
            <FlagMessage className="sw-mt-2" variant="warning">
              {translateWithParameters('users.tokens.new_token_created', token)}
            </FlagMessage>
          </>
        ) : (
          <>
            <div className="sw-flex sw-pt-4">
              <Spinner loading={loading}>
                <div className="sw-flex-col sw-mr-2">
                  <label className="sw-block" htmlFor="token-name">
                    {translate('onboarding.token.name.label')}
                  </label>
                  <InputField
                    aria-label={translate('onboarding.token.name.label')}
                    onChange={this.handleTokenNameChange}
                    id="token-name"
                    placeholder={translate('onboarding.token.name.placeholder')}
                    value={tokenName}
                    type="text"
                  />
                </div>
                <div className="sw-flex-col">
                  <label htmlFor="token-expiration">{translate('users.tokens.expires_in')}</label>
                  <div className="sw-flex">
                    <InputSelect
                      size="medium"
                      id="token-expiration"
                      isSearchable={false}
                      onChange={(data: LabelValueSelectOption<TokenExpiration>) =>
                        this.handleTokenExpirationChange(data.value)
                      }
                      options={tokenExpirationOptions}
                      value={tokenExpirationOptions.find(
                        (option) => option.value === tokenExpiration,
                      )}
                    />
                    <ButtonSecondary
                      className="sw-ml-2"
                      disabled={!tokenName}
                      onClick={this.getNewToken}
                    >
                      {translate('onboarding.token.generate')}
                    </ButtonSecondary>
                  </div>
                </div>
              </Spinner>
            </div>
            {type === TokenType.Project && <ProjectTokenScopeInfo />}
          </>
        )}
      </div>
    );
  }

  render() {
    const { loading } = this.state;
    const type = this.getTokenType();
    const header = translate('onboarding.token.generate', type);

    return (
      <Modal
        onClose={this.props.onClose}
        headerTitle={header}
        isOverflowVisible
        loading={loading}
        body={this.renderForm(type)}
        secondaryButtonLabel={translate('continue')}
      />
    );
  }
}
