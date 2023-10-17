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
import {
  ButtonSecondary,
  FlagMessage,
  FormField,
  InputField,
  InputTextArea,
  Link,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../../helpers/docs';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { AlmBindingDefinitionBase } from '../../../../types/alm-settings';
import '../../styles.css';

export interface AlmBindingDefinitionFormFieldProps<B extends AlmBindingDefinitionBase> {
  autoFocus?: boolean;
  help?: React.ReactNode;
  id: string;
  isInvalid?: boolean;
  isTextArea?: boolean;
  maxLength?: number;
  onFieldChange: (id: keyof B, value: string) => void;
  optional?: boolean;
  overwriteOnly?: boolean;
  propKey: keyof B;
  value: string;
  isSecret?: boolean;
}

export function AlmBindingDefinitionFormField<B extends AlmBindingDefinitionBase>(
  props: Readonly<AlmBindingDefinitionFormFieldProps<B>>,
) {
  const {
    autoFocus,
    help,
    id,
    isInvalid = false,
    isTextArea,
    maxLength,
    optional,
    overwriteOnly = false,
    propKey,
    value,
    isSecret,
  } = props;
  const [showField, setShowField] = React.useState(!overwriteOnly);

  const toStatic = useDocUrl('/instance-administration/security/#settings-encryption');

  return (
    <FormField
      htmlFor={id}
      label={translate('settings.almintegration.form', id)}
      description={help}
      required={!optional}
      className="sw-mb-8"
    >
      {!showField && overwriteOnly && (
        <div className="sw-flex sw-items-center">
          <p className="sw-mr-2">{translate('settings.almintegration.form.secret.field')}</p>
          <ButtonSecondary
            aria-label={translateWithParameters(
              'settings.almintegration.form.secret.update_field_x',
              translate('settings.almintegration.form', id),
            )}
            onClick={() => {
              props.onFieldChange(propKey, '');
              setShowField(true);
            }}
          >
            {translate('settings.almintegration.form.secret.update_field')}
          </ButtonSecondary>
        </div>
      )}
      {showField && isTextArea && (
        <InputTextArea
          id={id}
          maxLength={maxLength || 2000}
          onChange={(e) => props.onFieldChange(propKey, e.currentTarget.value)}
          required={!optional}
          rows={5}
          size="full"
          value={value}
          isInvalid={isInvalid}
        />
      )}
      {showField && !isTextArea && (
        <InputField
          autoFocus={autoFocus}
          id={id}
          maxLength={maxLength || 100}
          name={id}
          onChange={(e) => props.onFieldChange(propKey, e.currentTarget.value)}
          type="text"
          size="full"
          value={value}
          isInvalid={isInvalid}
        />
      )}
      {showField && isSecret && (
        <FlagMessage variant="info" className="sw-mt-2">
          <span>
            <FormattedMessage
              id="settings.almintegration.form.secret.can_encrypt"
              defaultMessage={translate('settings.almintegration.form.secret.can_encrypt')}
              values={{
                learn_more: <Link to={toStatic}>{translate('learn_more')}</Link>,
              }}
            />
          </span>
        </FlagMessage>
      )}
    </FormField>
  );
}
