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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import { SubmitButton } from '../../../components/controls/buttons';
import { ClipboardButton } from '../../../components/controls/clipboard';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
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
            <div className="markdown">
              <ul>
                <li>
                  <FormattedMessage
                    defaultMessage={translate('encryption.how_to_use.content1')}
                    id="encryption.how_to_use.content1"
                    values={{
                      secret_file: <code>~/.sonar/sonar-secret.txt</code>,
                      property: <code>sonar.secretKeyPath</code>,
                      propreties_file: <code>conf/sonar.properties</code>,
                    }}
                  />
                </li>
                <li>{translate('encryption.how_to_use.content2')}</li>
                <li>
                  <FormattedMessage
                    defaultMessage={translate('encryption.how_to_use.content3')}
                    id="encryption.how_to_use.content3"
                    values={{
                      property: <code>sonar.secretKeyPath</code>,
                    }}
                  />
                </li>
                <li>{translate('encryption.how_to_use.content4')}</li>
              </ul>
            </div>
          </>
        ) : (
          <form id="generate-secret-key-form" onSubmit={this.handleSubmit}>
            <p className="spacer-bottom">
              <FormattedMessage
                defaultMessage={translate('encryption.secret_key_description')}
                id="encryption.secret_key_description"
                values={{
                  moreInformationLink: (
                    <DocLink to="https://knowledgebase.autorabit.com/codescan/docs">
                      {translate('more_information')}
                    </DocLink>
                  ),
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
