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
import InputValidationField from 'sonar-ui-common/components/controls/InputValidationField';
import ValidationModal from 'sonar-ui-common/components/controls/ValidationModal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isWebUri } from 'valid-url';

interface Props {
  onClose: () => void;
  onDone: (data: Values) => Promise<void>;
  webhook?: T.Webhook;
}

interface Values {
  name: string;
  secret: string;
  url: string;
}

export default class CreateWebhookForm extends React.PureComponent<Props> {
  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  handleValidate = (data: Values) => {
    const { name, secret, url } = data;
    const errors: { name?: string; secret?: string; url?: string } = {};
    if (!name.trim()) {
      errors.name = translate('webhooks.name.required');
    }
    if (!url.trim()) {
      errors.url = translate('webhooks.url.required');
    } else if (!url.startsWith('http://') && !url.startsWith('https://')) {
      errors.url = translate('webhooks.url.bad_protocol');
    } else if (!isWebUri(url)) {
      errors.url = translate('webhooks.url.bad_format');
    }
    if (secret && secret.length > 200) {
      errors.secret = translate('webhooks.secret.bad_format');
    }
    return errors;
  };

  render() {
    const { webhook } = this.props;
    const isUpdate = !!webhook;
    const modalHeader = isUpdate ? translate('webhooks.update') : translate('webhooks.create');
    const confirmButtonText = isUpdate ? translate('update_verb') : translate('create');
    return (
      <ValidationModal
        confirmButtonText={confirmButtonText}
        header={modalHeader}
        initialValues={{
          name: (webhook && webhook.name) || '',
          secret: (webhook && webhook.secret) || '',
          url: (webhook && webhook.url) || ''
        }}
        isInitialValid={isUpdate}
        onClose={this.props.onClose}
        onSubmit={this.props.onDone}
        size="small"
        validate={this.handleValidate}>
        {({ dirty, errors, handleBlur, handleChange, isSubmitting, touched, values }) => (
          <>
            <InputValidationField
              autoFocus={true}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.name}
              id="webhook-name"
              label={
                <label htmlFor="webhook-name">
                  {translate('webhooks.name')}
                  <em className="mandatory">*</em>
                </label>
              }
              name="name"
              onBlur={handleBlur}
              onChange={handleChange}
              touched={touched.name}
              type="text"
              value={values.name}
            />
            <InputValidationField
              description={translate('webhooks.url.description')}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.url}
              id="webhook-url"
              label={
                <label htmlFor="webhook-url">
                  {translate('webhooks.url')}
                  <em className="mandatory">*</em>
                </label>
              }
              name="url"
              onBlur={handleBlur}
              onChange={handleChange}
              touched={touched.url}
              type="text"
              value={values.url}
            />
            <InputValidationField
              description={translate('webhooks.secret.description')}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.secret}
              id="webhook-secret"
              label={<label htmlFor="webhook-secret">{translate('webhooks.secret')}</label>}
              name="secret"
              onBlur={handleBlur}
              onChange={handleChange}
              touched={touched.secret}
              type="password"
              value={values.secret}
            />
          </>
        )}
      </ValidationModal>
    );
  }
}
