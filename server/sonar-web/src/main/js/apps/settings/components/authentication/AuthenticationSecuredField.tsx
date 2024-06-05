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
import { ButtonSecondary, InputField, InputTextArea } from 'design-system';
import React, { useEffect } from 'react';
import { translate } from '../../../../helpers/l10n';
import { DefinitionV2, ExtendedSettingDefinition, SettingType } from '../../../../types/settings';
import { isSecuredDefinition } from '../../utils';

interface SamlToggleFieldProps {
  definition: ExtendedSettingDefinition | DefinitionV2;
  isNotSet: boolean;
  onFieldChange: (key: string, value: string) => void;
  optional?: boolean;
  settingValue?: string;
}

export default function AuthenticationSecuredField(props: SamlToggleFieldProps) {
  const { settingValue, definition, optional = true, isNotSet } = props;
  const [showSecretField, setShowSecretField] = React.useState(
    !isNotSet && isSecuredDefinition(definition),
  );

  useEffect(() => {
    setShowSecretField(!isNotSet && isSecuredDefinition(definition));
  }, [isNotSet, definition]);

  return (
    <>
      {!showSecretField &&
        (definition.type === SettingType.TEXT ? (
          <InputTextArea
            size="full"
            id={definition.key}
            maxLength={4000}
            onChange={(e) => props.onFieldChange(definition.key, e.currentTarget.value)}
            required={!optional}
            rows={5}
            value={settingValue ?? ''}
          />
        ) : (
          <InputField
            size="full"
            id={definition.key}
            maxLength={4000}
            name={definition.key}
            onChange={(e) => props.onFieldChange(definition.key, e.currentTarget.value)}
            type="text"
            value={String(settingValue ?? '')}
          />
        ))}
      {showSecretField && (
        <div className="sw-flex sw-items-center">
          <p className="sw-mr-2">{translate('settings.almintegration.form.secret.field')}</p>
          <ButtonSecondary
            onClick={() => {
              setShowSecretField(false);
            }}
          >
            {translate('settings.almintegration.form.secret.update_field')}
          </ButtonSecondary>
        </div>
      )}
    </>
  );
}
