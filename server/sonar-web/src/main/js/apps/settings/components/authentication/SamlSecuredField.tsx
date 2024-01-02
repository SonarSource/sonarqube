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
import React, { useEffect } from 'react';
import { ButtonLink } from '../../../../components/controls/buttons';
import { translate } from '../../../../helpers/l10n';
import { ExtendedSettingDefinition, SettingValue } from '../../../../types/settings';

interface SamlToggleFieldProps {
  onFieldChange: (key: string, value: string) => void;
  settingValue?: SettingValue;
  definition: ExtendedSettingDefinition;
  optional?: boolean;
  showTextArea: boolean;
}

export default function SamlSecuredField(props: SamlToggleFieldProps) {
  const { settingValue, definition, optional = true, showTextArea } = props;
  const [showField, setShowField] = React.useState(showTextArea);

  useEffect(() => {
    setShowField(showTextArea);
  }, [showTextArea]);

  return (
    <>
      {showField && (
        <textarea
          className="width-100"
          id={definition.key}
          maxLength={4000}
          onChange={(e) => props.onFieldChange(definition.key, e.currentTarget.value)}
          required={!optional}
          rows={5}
          value={settingValue?.value ?? ''}
        />
      )}
      {!showField && (
        <div>
          <p>{translate('settings.almintegration.form.secret.field')}</p>
          <ButtonLink
            onClick={() => {
              setShowField(true);
            }}
          >
            {translate('settings.almintegration.form.secret.update_field')}
          </ButtonLink>
        </div>
      )}
    </>
  );
}
