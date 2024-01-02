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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  checkPersonalAccessTokenIsValid,
  setAlmPersonalAccessToken,
} from '../../../api/alm-integrations';
import { SubmitButton } from '../../../components/controls/buttons';
import ValidationInput from '../../../components/controls/ValidationInput';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { tokenExistedBefore } from './utils';

interface Props {
  almSetting: AlmSettingsInstance;
  resetPat: boolean;
  onPersonalAccessTokenCreated: () => void;
}

interface State {
  validationFailed: boolean;
  validationErrorMessage?: string;
  touched: boolean;
  password: string;
  username?: string;
  submitting: boolean;
  checkingPat: boolean;
  firstConnection: boolean;
}

function getPatUrl(alm: AlmKeys, url = '') {
  if (alm === AlmKeys.BitbucketServer) {
    return `${url.replace(/\/$/, '')}/account`;
  } else if (alm === AlmKeys.BitbucketCloud) {
    return 'https://bitbucket.org/account/settings/app-passwords/new';
  } else if (alm === AlmKeys.GitLab) {
    return 'https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html';
  }

  return '';
}

export default class PersonalAccessTokenForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      checkingPat: false,
      touched: false,
      password: '',
      submitting: false,
      validationFailed: false,
      firstConnection: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.checkPATAndUpdateView();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.almSetting !== prevProps.almSetting) {
      this.checkPATAndUpdateView();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkPATAndUpdateView = async () => {
    const {
      almSetting: { key },
      resetPat,
    } = this.props;

    // We don't need to check PAT if we want to reset
    if (!resetPat) {
      this.setState({ checkingPat: true });
      const { patIsValid, error } = await checkPersonalAccessTokenIsValid(key)
        .then(({ status, error }) => ({ patIsValid: status, error }))
        .catch(() => ({ patIsValid: status, error: translate('default_error_message') }));
      if (patIsValid) {
        this.props.onPersonalAccessTokenCreated();
      }
      if (this.mounted) {
        // This is the initial message when no token was provided
        if (tokenExistedBefore(error)) {
          this.setState({
            checkingPat: false,
            firstConnection: true,
          });
        } else {
          this.setState({
            checkingPat: false,
            validationFailed: true,
            validationErrorMessage: error,
          });
        }
      }
    }
  };

  handleUsernameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({
      touched: true,
      username: event.target.value,
    });
  };

  handlePasswordChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({
      touched: true,
      password: event.target.value,
    });
  };

  handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    const { password, username } = this.state;
    const {
      almSetting: { key },
    } = this.props;

    e.preventDefault();
    if (password) {
      this.setState({ submitting: true });

      await setAlmPersonalAccessToken(key, password, username).catch(() => {
        /* Set will not check pat validity. We need to check again so we will catch issue after */
      });

      const { status, error } = await checkPersonalAccessTokenIsValid(key)
        .then(({ status, error }) => ({ status, error }))
        .catch(() => ({ status: false, error: translate('default_error_message') }));

      if (this.mounted && status) {
        // Let's reset status,
        this.setState({
          checkingPat: false,
          touched: false,
          password: '',
          submitting: false,
          username: '',
          validationFailed: false,
        });
        this.props.onPersonalAccessTokenCreated();
      } else if (this.mounted) {
        this.setState({
          submitting: false,
          touched: false,
          validationFailed: true,
          validationErrorMessage: error,
        });
      }
    }
  };

  renderHelpBox(suffixTranslationKey: string) {
    const {
      almSetting: { alm, url },
    } = this.props;

    return (
      <Alert className="big-spacer-left width-50" display="block" variant="info">
        {alm === AlmKeys.BitbucketCloud && (
          <>
            <h3>
              {translate(
                'onboarding.create_project.pat_help.instructions_username.bitbucketcloud.title'
              )}
            </h3>
            <p className="big-spacer-top big-spacer-bottom">
              {translate('onboarding.create_project.pat_help.instructions_username.bitbucketcloud')}
            </p>

            <div className="text-middle big-spacer-bottom">
              <img
                alt="" // Should be ignored by screen readers
                className="spacer-right"
                height="16"
                src={`${getBaseUrl()}/images/alm/${AlmKeys.BitbucketServer}.svg`}
              />
              <a
                href="https://bitbucket.org/account/settings/"
                rel="noopener noreferrer"
                target="_blank"
              >
                {translate(
                  'onboarding.create_project.pat_help.instructions_username.bitbucketcloud.link'
                )}
              </a>
            </div>
          </>
        )}

        <h3>{translate(`onboarding.create_project.pat_help${suffixTranslationKey}.title`)}</h3>

        <p className="big-spacer-top big-spacer-bottom">
          {alm === AlmKeys.BitbucketServer ? (
            <FormattedMessage
              id="onboarding.create_project.pat_help.instructions"
              defaultMessage={translate(
                `onboarding.create_project.pat_help.bitbucket.instructions`
              )}
              values={{
                menu: (
                  <strong>
                    {translate('onboarding.create_project.pat_help.bitbucket.instructions.menu')}
                  </strong>
                ),
                button: (
                  <strong>
                    {translate('onboarding.create_project.pat_help.bitbucket.instructions.button')}
                  </strong>
                ),
              }}
            />
          ) : (
            <FormattedMessage
              id="onboarding.create_project.pat_help.instructions"
              defaultMessage={translate(
                `onboarding.create_project.pat_help${suffixTranslationKey}.instructions`
              )}
              values={{
                alm: translate('onboarding.alm', alm),
              }}
            />
          )}
        </p>

        {(url || alm === AlmKeys.BitbucketCloud) && (
          <div className="text-middle">
            <img
              alt="" // Should be ignored by screen readers
              className="spacer-right"
              height="16"
              src={`${getBaseUrl()}/images/alm/${
                alm === AlmKeys.BitbucketCloud ? AlmKeys.BitbucketServer : alm
              }.svg`}
            />
            <a href={getPatUrl(alm, url)} rel="noopener noreferrer" target="_blank">
              {translate(`onboarding.create_project.pat_help${suffixTranslationKey}.link`)}
            </a>
          </div>
        )}

        <p className="big-spacer-top big-spacer-bottom">
          {translate('onboarding.create_project.pat_help.instructions2', alm)}
        </p>

        <ul>
          {alm === AlmKeys.BitbucketServer && (
            <li>
              <FormattedMessage
                defaultMessage={translate(
                  'onboarding.create_project.pat_help.bbs_permission_projects'
                )}
                id="onboarding.create_project.pat_help.bbs_permission_projects"
                values={{
                  perm: (
                    <strong>
                      {translate('onboarding.create_project.pat_help.read_permission')}
                    </strong>
                  ),
                }}
              />
            </li>
          )}
          {(alm === AlmKeys.BitbucketServer || alm === AlmKeys.BitbucketCloud) && (
            <li>
              <FormattedMessage
                defaultMessage={translate(
                  'onboarding.create_project.pat_help.bbs_permission_repos'
                )}
                id="onboarding.create_project.pat_help.bbs_permission_repos"
                values={{
                  perm: (
                    <strong>
                      {translate('onboarding.create_project.pat_help.read_permission')}
                    </strong>
                  ),
                }}
              />
            </li>
          )}

          {alm === AlmKeys.GitLab && (
            <li className="spacer-bottom">
              <strong>
                {translate('onboarding.create_project.pat_help.gitlab.read_api_permission')}
              </strong>
            </li>
          )}
        </ul>
      </Alert>
    );
  }

  render() {
    const {
      almSetting: { alm },
    } = this.props;
    const {
      checkingPat,
      submitting,
      touched,
      password,
      username,
      validationFailed,
      validationErrorMessage,
      firstConnection,
    } = this.state;

    if (checkingPat) {
      return <DeferredSpinner className="spacer-left" loading={true} />;
    }

    const suffixTranslationKey = alm === AlmKeys.BitbucketCloud ? '.bitbucketcloud' : '';

    const isInvalid = validationFailed && !touched;
    const canSubmit = Boolean(password) && (alm !== AlmKeys.BitbucketCloud || Boolean(username));
    const submitButtonDiabled = isInvalid || submitting || !canSubmit;

    const errorMessage =
      validationErrorMessage ?? translate('onboarding.create_project.pat_incorrect', alm);

    return (
      <div className="display-flex-start">
        <form className="width-50" onSubmit={this.handleSubmit}>
          <h2 className="big">{translate('onboarding.create_project.pat_form.title', alm)}</h2>
          <p className="big-spacer-top big-spacer-bottom">
            {translate('onboarding.create_project.pat_form.help', alm)}
          </p>

          {!firstConnection && (
            <Alert className="big-spacer-right" variant="warning">
              <p>{translate('onboarding.create_project.pat.expired.info_message')}</p>
              <p>{translate('onboarding.create_project.pat.expired.info_message_contact')}</p>
            </Alert>
          )}

          {alm === AlmKeys.BitbucketCloud && (
            <ValidationInput
              error={undefined}
              labelHtmlFor="enter_username_validation"
              isInvalid={false}
              isValid={false}
              label={translate('onboarding.create_project.enter_username')}
              required={true}
            >
              <input
                autoFocus={true}
                className={classNames('input-super-large', {
                  'is-invalid': isInvalid,
                })}
                id="enter_username_validation"
                minLength={1}
                name="username"
                value={username}
                onChange={this.handleUsernameChange}
                type="text"
              />
            </ValidationInput>
          )}

          <ValidationInput
            error={errorMessage}
            labelHtmlFor="personal_access_token_validation"
            isInvalid={false}
            isValid={false}
            label={translate(`onboarding.create_project.enter_pat${suffixTranslationKey}`)}
            required={true}
          >
            <input
              autoFocus={alm !== AlmKeys.BitbucketCloud}
              className={classNames('input-super-large', {
                'is-invalid': isInvalid,
              })}
              id="personal_access_token_validation"
              minLength={1}
              value={password}
              onChange={this.handlePasswordChange}
              type="text"
            />
          </ValidationInput>

          <ValidationInput
            error={errorMessage}
            labelHtmlFor="personal_access_token_submit"
            isInvalid={isInvalid}
            isValid={false}
            label={null}
          >
            <SubmitButton disabled={submitButtonDiabled}>{translate('save')}</SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </ValidationInput>
        </form>

        {this.renderHelpBox(suffixTranslationKey)}
      </div>
    );
  }
}
