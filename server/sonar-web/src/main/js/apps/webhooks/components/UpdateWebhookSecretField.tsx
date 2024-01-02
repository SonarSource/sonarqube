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
import { DiscreetLink, FlagMessage } from 'design-system';
import { useField } from 'formik';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import InputValidationField from '../../../components/controls/InputValidationField';
import ModalValidationField from '../../../components/controls/ModalValidationField';
import { translate } from '../../../helpers/l10n';

interface Props {
  description?: string;
  dirty: boolean;
  disabled: boolean;
  error: string | undefined;
  id?: string;
  isUpdateForm: boolean;
  label?: React.ReactNode;
  name: string;
  onBlur: (event: React.FocusEvent<HTMLInputElement>) => void;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  touched: boolean | undefined;
  type?: string;
  value?: string;
}

export default function UpdateWebhookSecretField(props: Props) {
  const {
    isUpdateForm,
    description,
    dirty,
    disabled,
    error,
    id,
    label,
    name,
    onBlur,
    onChange,
    touched,
    type,
    value,
  } = props;

  const [isSecretInputDisplayed, setIsSecretInputDisplayed] = React.useState(false);
  const [, , { setValue: setSecretValue }] = useField('secret');

  const showSecretInput = () => {
    setSecretValue('');
    setIsSecretInputDisplayed(true);
  };

  return !isUpdateForm || isSecretInputDisplayed ? (
    <InputValidationField
      description={description}
      dirty={dirty}
      disabled={disabled}
      error={error}
      id={id}
      label={label}
      name={name}
      onBlur={onBlur}
      onChange={onChange}
      touched={touched}
      type={type}
      value={value as string}
    />
  ) : (
    <ModalValidationField
      description={description}
      dirty={false}
      error={undefined}
      label={label}
      touched
    >
      {() => (
        <div>
          <FlagMessage variant="info" className="sw-w-full">
            <FormattedMessage
              defaultMessage={translate('webhooks.secret.field_mask.description')}
              id="webhooks.secret.field_mask.description"
              values={{
                link: (
                  <DiscreetLink
                    className="sw-ml-1"
                    onClick={showSecretInput}
                    preventDefault
                    to={{}}
                  >
                    {translate('webhooks.secret.field_mask.link')}
                  </DiscreetLink>
                ),
              }}
            />
          </FlagMessage>
        </div>
      )}
    </ModalValidationField>
  );
}
