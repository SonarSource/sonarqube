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
import { isWebUri } from 'valid-url';
import InputValidationField from '../../../components/controls/InputValidationField';
import ValidationModal from '../../../components/controls/ValidationModal';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { WebhookBasePayload, WebhookResponse } from '../../../types/webhook';
import UpdateWebhookSecretField from './UpdateWebhookSecretField';

interface Props {
  onClose: () => void;
  onDone: (data: WebhookBasePayload) => Promise<void>;
  webhook?: WebhookResponse;
}

export default class CreateWebhookForm extends React.PureComponent<Props> {
  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  handleValidate = (data: WebhookBasePayload) => {
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
          name: webhook?.name ?? '',
          url: webhook?.url ?? '',
          secret: isUpdate ? undefined : '',
        }}
        onClose={this.props.onClose}
        onSubmit={this.props.onDone}
        size="small"
        validate={this.handleValidate}
      >
        {({ dirty, errors, handleBlur, handleChange, isSubmitting, touched, values }) => (
          <>
            <MandatoryFieldsExplanation className="big-spacer-bottom" />

            <InputValidationField
              autoFocus={true}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.name}
              id="webhook-name"
              label={
                <label htmlFor="webhook-name">
                  {translate('webhooks.name')}
                  <MandatoryFieldMarker />
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
                  <MandatoryFieldMarker />
                </label>
              }
              name="url"
              onBlur={handleBlur}
              onChange={handleChange}
              touched={touched.url}
              type="text"
              value={values.url}
            />
            <UpdateWebhookSecretField
              description={`${translate('webhooks.secret.description')}${
                isUpdate ? ` ${translate('webhooks.secret.description.update')}` : ''
              }`}
              dirty={dirty}
              disabled={isSubmitting}
              error={errors.secret}
              id="webhook-secret"
              isUpdateForm={isUpdate}
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
