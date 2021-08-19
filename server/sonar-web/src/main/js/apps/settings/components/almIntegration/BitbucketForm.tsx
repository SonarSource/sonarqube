/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import RadioToggle from '../../../../components/controls/RadioToggle';
import { Alert } from '../../../../components/ui/Alert';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import { translate } from '../../../../helpers/l10n';
import {
  AlmKeys,
  BitbucketCloudBindingDefinition,
  BitbucketServerBindingDefinition
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
          <RadioToggle
            className="little-spacer-top big-spacer-bottom"
            name="variant"
            onCheck={props.onVariantChange}
            options={[
              {
                label: 'Bitbucket Server',
                value: AlmKeys.BitbucketServer
              },
              { label: 'Bitbucket Cloud', value: AlmKeys.BitbucketCloud }
            ]}
            value={variant}
          />
        </div>
      )}

      {variant !== undefined && (
        <>
          {variant === AlmKeys.BitbucketServer && (
            <div className="display-flex-start">
              <div className="flex-1">
                <BitbucketServerForm
                  onFieldChange={props.onFieldChange}
                  formData={formData as BitbucketServerBindingDefinition}
                />
              </div>
              <Alert className="huge-spacer-left flex-1" variant="info">
                <>
                  <h3>{translate('onboarding.create_project.pat_help.title')}</h3>

                  <p className="big-spacer-top">
                    {translate('settings.almintegration.bitbucket.help_1')}
                  </p>

                  <ul className="big-spacer-top list-styled">
                    <li>{translate('settings.almintegration.bitbucket.help_2')}</li>
                    <li>{translate('settings.almintegration.bitbucket.help_3')}</li>
                  </ul>

                  <p className="big-spacer-top big-spacer-bottom">
                    <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.BitbucketServer]}>
                      {translate('learn_more')}
                    </Link>
                  </p>
                </>
              </Alert>
            </div>
          )}

          {variant === AlmKeys.BitbucketCloud && (
            <div className="display-flex-start">
              <div className="flex-1">
                <BitbucketCloudForm
                  onFieldChange={props.onFieldChange}
                  formData={formData as BitbucketCloudBindingDefinition}
                />
              </div>
              <Alert className="huge-spacer-left flex-1" variant="info">
                <FormattedMessage
                  defaultMessage={translate(`settings.almintegration.bitbucketcloud.info`)}
                  id="settings.almintegration.bitbucketcloud.info"
                  values={{
                    link: (
                      <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.BitbucketCloud]}>
                        {translate('learn_more')}
                      </Link>
                    )
                  }}
                />
              </Alert>
            </div>
          )}
        </>
      )}
    </>
  );
}
