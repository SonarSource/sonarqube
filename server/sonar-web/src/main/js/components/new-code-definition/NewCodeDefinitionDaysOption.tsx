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
import { translate, translateWithParameters } from '../../helpers/l10n';
import {
  NUMBER_OF_DAYS_MAX_VALUE,
  NUMBER_OF_DAYS_MIN_VALUE,
} from '../../helpers/new-code-definition';
import { NewCodeDefinitionType } from '../../types/new-code-definition';
import RadioCard from '../controls/RadioCard';
import ValidationInput, { ValidationInputErrorPlacement } from '../controls/ValidationInput';
import MandatoryFieldsExplanation from '../ui/MandatoryFieldsExplanation';

export interface Props {
  className?: string;
  days: string;
  disabled?: boolean;
  isChanged: boolean;
  isValid: boolean;
  onChangeDays: (value: string) => void;
  onSelect: (selection: NewCodeDefinitionType) => void;
  selected: boolean;
}

export default function NewCodeDefinitionDaysOption(props: Props) {
  const { className, days, disabled, isChanged, isValid, onChangeDays, onSelect, selected } = props;

  return (
    <RadioCard
      noRadio
      className={className}
      disabled={disabled}
      onClick={() => onSelect(NewCodeDefinitionType.NumberOfDays)}
      selected={selected}
      title={translate('new_code_definition.number_days')}
    >
      <>
        <div>
          <p className="sw-mb-3">{translate('new_code_definition.number_days.description')}</p>
          <p className="sw-mb-4">{translate('new_code_definition.number_days.usecase')}</p>
        </div>
        {selected && (
          <>
            <MandatoryFieldsExplanation />

            <ValidationInput
              labelHtmlFor="baseline_number_of_days"
              isInvalid={!isValid}
              isValid={isChanged && isValid}
              errorPlacement={ValidationInputErrorPlacement.Bottom}
              error={translateWithParameters(
                'new_code_definition.number_days.invalid',
                NUMBER_OF_DAYS_MIN_VALUE,
                NUMBER_OF_DAYS_MAX_VALUE
              )}
              label={translate('new_code_definition.number_days.specify_days')}
              required
            >
              <input
                id="baseline_number_of_days"
                onChange={(e) => onChangeDays(e.currentTarget.value)}
                type="text"
                value={days}
              />
            </ValidationInput>
          </>
        )}
      </>
    </RadioCard>
  );
}
