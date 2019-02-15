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
import { FormikProps } from 'formik';
import { isWebUri } from 'valid-url';
import ValidationModal from '../../../components/controls/ValidationModal';
import InputValidationField from '../../../components/controls/InputValidationField';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onDone: (data: { name: string; url: string }) => Promise<void>;
  webhook?: T.Webhook;
}

interface Values {
  name: string;
  url: string;
}

export default class CreateWebhookForm extends React.PureComponent<Props> {
  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  handleValidate = (data: Values) => {
    const { name, url } = data;
    const errors: { name?: string; url?: string } = {};
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
          name: webhook ? webhook.name : '',
          url: webhook ? webhook.url : ''
        }}
        isInitialValid={isUpdate}
        onClose={this.props.onClose}
        onSubmit={this.props.onDone}
        size="small"
        validate={this.handleValidate}>
        {({
          dirty,
          errors,
          handleBlur,
          handleChange,
          isSubmitting,
          touched,
          values
        }: FormikProps<Values>) => (
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
          </>
        )}
      </ValidationModal>
    );
  }
}
