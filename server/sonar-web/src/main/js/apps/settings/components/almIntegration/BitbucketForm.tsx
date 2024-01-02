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
import * as React from 'react';
import ButtonToggle from '../../../../components/controls/ButtonToggle';
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
    value: string
  ) => void;
  variant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
  onVariantChange: (variant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud) => void;
}

export default function BitbucketForm(props: BitbucketFormProps) {
  const { isUpdate, formData, variant } = props;

  return (
    <>
      {!isUpdate && (
        <div className="display-flex-column">
          <strong>{translate('settings.almintegration.form.choose_bitbucket_variant')}</strong>
          <div className="little-spacer-top big-spacer-bottom">
            <ButtonToggle
              label={translate('settings.almintegration.form.choose_bitbucket_variant')}
              onCheck={props.onVariantChange}
              options={[
                {
                  label: 'Bitbucket Server',
                  value: AlmKeys.BitbucketServer,
                },
                { label: 'Bitbucket Cloud', value: AlmKeys.BitbucketCloud },
              ]}
              value={variant}
            />
          </div>
        </div>
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
