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
import styled from '@emotion/styled';
import {
  FlagErrorIcon,
  FormField,
  Highlight,
  InputField,
  Note,
  RequiredIcon,
  TextError,
} from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { isDefined } from '../../../../helpers/types';
import { DefinitionV2, ExtendedSettingDefinition, SettingType } from '../../../../types/settings';
import { getPropertyDescription, getPropertyName, isSecuredDefinition } from '../../utils';
import AuthenticationMultiValueField from './AuthenticationMultiValuesField';
import AuthenticationSecuredField from './AuthenticationSecuredField';
import AuthenticationToggleFormField from './AuthenticationToggleField';

interface Props {
  className?: string;
  definition: ExtendedSettingDefinition | DefinitionV2;
  error?: string;
  isNotSet: boolean;
  mandatory?: boolean;
  onFieldChange: (key: string, value: string | boolean | string[]) => void;
  settingValue?: string | boolean | string[];
}

export default function AuthenticationFormField(props: Readonly<Props>) {
  const { mandatory = false, definition, settingValue, isNotSet, error, className } = props;

  const intl = useIntl();

  const name = getPropertyName(definition);
  const description = getPropertyDescription(definition);

  if (!isSecuredDefinition(definition) && definition.type === SettingType.BOOLEAN) {
    return (
      <>
        <div className="sw-flex">
          <Highlight className="sw-mb-4 sw-mr-4 sw-flex sw-items-center sw-gap-2">
            <StyledLabel aria-label={name} htmlFor={definition.key}>
              {name}
              {mandatory && (
                <RequiredIcon
                  aria-label={intl.formatMessage({ id: 'required' })}
                  className="sw-ml-1"
                />
              )}
            </StyledLabel>
          </Highlight>
          <AuthenticationToggleFormField
            definition={definition}
            settingValue={settingValue as string | boolean}
            onChange={(value) => props.onFieldChange(definition.key, value)}
          />
        </div>
        {description && <Note className="sw-mt-2">{description}</Note>}
      </>
    );
  }

  return (
    <FormField
      className={className}
      htmlFor={definition.key}
      ariaLabel={name}
      label={name}
      description={description}
      required={mandatory}
    >
      {definition.multiValues && (
        <AuthenticationMultiValueField
          definition={definition}
          settingValue={settingValue as string[]}
          onFieldChange={(value) => props.onFieldChange(definition.key, value)}
        />
      )}
      {isSecuredDefinition(definition) && (
        <AuthenticationSecuredField
          definition={definition}
          settingValue={String(settingValue ?? '')}
          onFieldChange={props.onFieldChange}
          isNotSet={isNotSet}
        />
      )}
      {!isSecuredDefinition(definition) &&
        definition.type === undefined &&
        !definition.multiValues && (
          <>
            <InputField
              size="full"
              id={definition.key}
              isInvalid={isDefined(error) && error !== ''}
              maxLength={4000}
              name={definition.key}
              onChange={(e) => props.onFieldChange(definition.key, e.currentTarget.value)}
              type="text"
              value={String(settingValue ?? '')}
            />
            {isDefined(error) && error !== '' && (
              <TextError
                className="sw-mt-2"
                text={
                  <>
                    <FlagErrorIcon className="sw-mr-1" />
                    {error}
                  </>
                }
              />
            )}
          </>
        )}
    </FormField>
  );
}

// This is needed to prevent the target input/button from being focused
// when clicking/hovering on the label. More info https://stackoverflow.com/questions/9098581/why-is-hover-for-input-triggered-on-corresponding-label-in-css
const StyledLabel = styled.label`
  pointer-events: none;
`;
