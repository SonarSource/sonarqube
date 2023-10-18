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
import { FormField, ToggleButton } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  AlmKeys,
  BitbucketCloudBindingDefinition,
  BitbucketServerBindingDefinition,
} from '../../../../types/alm-settings';
import BitbucketCloudForm from './BitbucketCloudForm';
import BitbucketServerForm from './BitbucketServerForm';

export interface BitbucketFormProps {
  formData: BitbucketServerBindingDefinition | BitbucketCloudBindingDefinition;
  isUpdate: boolean;
  onFieldChange: (
    fieldId: keyof (BitbucketServerBindingDefinition & BitbucketCloudBindingDefinition),
    value: string,
  ) => void;
  variant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
  onVariantChange: (variant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud) => void;
}

export default function BitbucketForm(props: Readonly<BitbucketFormProps>) {
  const { isUpdate, formData, variant } = props;

  return (
    <>
      {!isUpdate && (
        <FormField label={translate('settings.almintegration.form.choose_bitbucket_variant')}>
          <div>
            <ToggleButton
              label={translate('settings.almintegration.form.choose_bitbucket_variant')}
              onChange={props.onVariantChange}
              options={[
                {
                  label: translate('alm.bitbucket.long'),
                  value: AlmKeys.BitbucketServer,
                },
                { label: translate('alm.bitbucketcloud.long'), value: AlmKeys.BitbucketCloud },
              ]}
              value={variant}
            />
          </div>
        </FormField>
      )}

      {variant !== undefined && (
        <>
          {variant === AlmKeys.BitbucketServer && (
            <BitbucketServerForm
              onFieldChange={props.onFieldChange}
              formData={formData as BitbucketServerBindingDefinition}
            />
          )}

          {variant === AlmKeys.BitbucketCloud && (
            <BitbucketCloudForm
              onFieldChange={props.onFieldChange}
              formData={formData as BitbucketCloudBindingDefinition}
            />
          )}
        </>
      )}
    </>
  );
}
