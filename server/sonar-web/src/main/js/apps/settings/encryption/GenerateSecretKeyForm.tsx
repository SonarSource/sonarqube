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
import ClipboardButton from '../../../components/controls/ClipboardButton';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  generateSecretKey: () => Promise<void>;
  secretKey?: string;
}

interface State {
  submitting: boolean;
}

export default class GenerateSecretKeyForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { submitting: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    this.props.generateSecretKey().then(this.stopSubmitting, this.stopSubmitting);
  };

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  render() {
    const { secretKey } = this.props;
    const { submitting } = this.state;
    return (
      <div id="generate-secret-key-form-container">
        {secretKey ? (
          <>
            <div className="big-spacer-bottom">
              <h3 className="spacer-bottom">{translate('encryption.secret_key')}</h3>
              <input
                className="input-clear input-code input-large"
                id="secret-key"
                readOnly={true}
                type="text"
                value={secretKey}
              />
              <ClipboardButton className="little-spacer-left" copyValue={secretKey} />
            </div>
            <h3 className="spacer-bottom">{translate('encryption.how_to_use')}</h3>
            <div
              className="markdown"
              dangerouslySetInnerHTML={{ __html: translate('encryption.how_to_use.content') }}
            />
          </>
        ) : (
          <form id="generate-secret-key-form" onSubmit={this.handleSubmit}>
            <p className="spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('encryption.secret_key_description')}
                id="encryption.secret_key_description"
                values={{
                  moreInformationLink: (
                    <a
                      href="https://redirect.sonarsource.com/doc/settings-encryption.html"
                      rel="noopener noreferrer"
                      target="_blank">
                      {translate('more_information')}
                    </a>
                  )
                }}
              />
            </p>
            <SubmitButton disabled={submitting}>
              {translate('encryption.generate_secret_key')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </form>
        )}
      </div>
    );
  }
}
