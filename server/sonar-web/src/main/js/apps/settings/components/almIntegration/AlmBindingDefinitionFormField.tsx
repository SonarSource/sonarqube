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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmBindingDefinition } from '../../../../types/alm-settings';

export interface AlmBindingDefinitionFormFieldProps<B extends AlmBindingDefinition> {
  autoFocus?: boolean;
  help?: React.ReactNode;
  id: string;
  isTextArea?: boolean;
  maxLength?: number;
  onFieldChange: (id: keyof B, value: string) => void;
  optional?: boolean;
  overwriteOnly?: boolean;
  propKey: keyof B;
  value: string;
}

export function AlmBindingDefinitionFormField<B extends AlmBindingDefinition>(
  props: AlmBindingDefinitionFormFieldProps<B>
) {
  const {
    autoFocus,
    help,
    id,
    isTextArea,
    maxLength,
    optional,
    overwriteOnly = false,
    propKey,
    value
  } = props;
  const [showField, setShowField] = React.useState(!overwriteOnly);

  return (
    <div className="modal-field">
      <label className="display-flex-center" htmlFor={id}>
        {translate('settings.almintegration.form', id)}
        {!optional && <MandatoryFieldMarker />}
        {help && <HelpTooltip className="spacer-left" overlay={help} placement="right" />}
      </label>

      {!showField && overwriteOnly && (
        <div>
          <p>{translate('settings.almintegration.form.secret_field')}</p>
          <ButtonLink
            onClick={() => {
              props.onFieldChange(propKey, '');
              setShowField(true);
            }}>
            {translate('settings.almintegration.form.update_secret_field')}
          </ButtonLink>
        </div>
      )}

      {showField && isTextArea && (
        <textarea
          className="settings-large-input"
          id={id}
          maxLength={maxLength || 2000}
          onChange={e => props.onFieldChange(propKey, e.currentTarget.value)}
          required={!optional}
          rows={5}
          value={value}
        />
      )}

      {showField && !isTextArea && (
        <input
          autoFocus={autoFocus}
          className="input-super-large"
          id={id}
          maxLength={maxLength || 100}
          name={id}
          onChange={e => props.onFieldChange(propKey, e.currentTarget.value)}
          size={50}
          type="text"
          value={value}
        />
      )}
    </div>
  );
}
