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
import { DestructiveIcon, InputField, TrashIcon } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../../helpers/l10n';
import { DefinitionV2, ExtendedSettingDefinition } from '../../../../types/settings';
import { getPropertyName } from '../../utils';

interface Props {
  definition: ExtendedSettingDefinition | DefinitionV2;
  onFieldChange: (value: string[]) => void;
  settingValue?: string[];
}

export default function AuthenticationMultiValueField(props: Props) {
  const { settingValue = [], definition } = props;

  const displayValue = [...settingValue, ''];

  const handleSingleInputChange = (index: number, value: string) => {
    const newValue = [...settingValue];
    newValue.splice(index, 1, value);
    props.onFieldChange(newValue);
  };

  const handleDeleteValue = (index: number) => {
    const newValue = [...settingValue];
    newValue.splice(index, 1);
    props.onFieldChange(newValue);
  };

  return (
    <div>
      <ul>
        {displayValue.map((value, index) => {
          const isNotLast = index !== displayValue.length - 1;
          return (
            <li className="sw-flex sw-mb-2" key={index}>
              <InputField
                id={definition.key}
                size="large"
                maxLength={4000}
                name={definition.key}
                onChange={(e) => handleSingleInputChange(index, e.currentTarget.value)}
                type="text"
                value={displayValue[index]}
              />

              {isNotLast && (
                <div className="sw-ml-2">
                  <DestructiveIcon
                    Icon={TrashIcon}
                    className="js-remove-value"
                    aria-label={translateWithParameters(
                      'settings.definition.delete_value',
                      getPropertyName(definition),
                      value,
                    )}
                    onClick={() => handleDeleteValue(index)}
                  />
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
