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
import React from 'react';
import ValidationInput, {
  ValidationInputErrorPlacement,
} from '../../../../components/controls/ValidationInput';
import MandatoryFieldMarker from '../../../../components/ui/MandatoryFieldMarker';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../../types/settings';
import SamlSecuredField from './SamlSecuredField';
import SamlToggleField from './SamlToggleField';

interface SamlToggleFieldProps {
  settingValue?: SettingValue;
  definition: ExtendedSettingDefinition;
  mandatory?: boolean;
  onFieldChange: (key: string, value: string | boolean) => void;
  showSecuredTextArea?: boolean;
  error: { [key: string]: string };
}

export default function SamlFormField(props: SamlToggleFieldProps) {
  const { mandatory = false, definition, settingValue, showSecuredTextArea = true, error } = props;

  return (
    <div className="settings-definition" key={definition.key}>
      <div className="settings-definition-left">
        <label className="h3" htmlFor={definition.key}>
          {definition.name}
        </label>
        {mandatory && <MandatoryFieldMarker />}
        {definition.description && (
          <div className="markdown small spacer-top">{definition.description}</div>
        )}
      </div>
      <div className="settings-definition-right big-padded-top display-flex-column">
        {definition.type === SettingType.PASSWORD && (
          <SamlSecuredField
            definition={definition}
            settingValue={settingValue}
            onFieldChange={props.onFieldChange}
            showTextArea={showSecuredTextArea}
          />
        )}
        {definition.type === SettingType.BOOLEAN && (
          <SamlToggleField
            definition={definition}
            settingValue={settingValue}
            toggleDisabled={false}
            onChange={(value) => props.onFieldChange(definition.key, value)}
          />
        )}
        {definition.type === undefined && (
          <ValidationInput
            error={error[definition.key]}
            errorPlacement={ValidationInputErrorPlacement.Bottom}
            isValid={false}
            isInvalid={Boolean(error[definition.key])}
          >
            <input
              className="width-100"
              id={definition.key}
              maxLength={4000}
              name={definition.key}
              onChange={(e) => props.onFieldChange(definition.key, e.currentTarget.value)}
              type="text"
              value={settingValue?.value ?? ''}
              aria-label={definition.key}
            />
          </ValidationInput>
        )}
      </div>
    </div>
  );
}
