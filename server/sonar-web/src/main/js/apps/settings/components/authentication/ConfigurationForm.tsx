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

import { Button, ButtonVariety, Spinner } from '@sonarsource/echoes-react';
import { FlagMessage, Modal } from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { AlmAuthDocLinkKeys } from '../../../../helpers/doc-links';
import { translate } from '../../../../helpers/l10n';
import { useSaveValuesMutation } from '../../../../queries/settings';
import { Dict } from '../../../../types/types';
import { AuthenticationTabs } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import { SettingValue } from './hook/useConfiguration';

interface Props {
  canBeSave: boolean;
  create: boolean;
  excludedField: string[];
  loading: boolean;
  onClose: () => void;
  setNewValue: (key: string, value: string | boolean) => void;
  tab: AuthenticationTabs;
  values: Dict<SettingValue>;
}

interface ErrorValue {
  key: string;
  message: string;
}

const FORM_ID = 'configuration-form';

export default function ConfigurationForm(props: Readonly<Props>) {
  const { canBeSave, create, excludedField, loading, setNewValue, tab, values } = props;
  const [errors, setErrors] = React.useState<Dict<ErrorValue>>({});

  const { mutateAsync: changeConfig } = useSaveValuesMutation();

  const header = translate('settings.authentication.form', create ? 'create' : 'edit', tab);

  const handleSubmit = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (canBeSave) {
      await onSave();
    } else {
      const errors = Object.values(values)
        .filter((v) => v.newValue === undefined && v.value === undefined && v.mandatory)
        .map((v) => ({ key: v.key, message: translate('field_required') }));

      setErrors(keyBy(errors, 'key'));
    }
  };

  const onSave = async () => {
    const data = await changeConfig(Object.values(values));

    const errors = data
      .filter(({ success }) => !success)
      .map(({ key }) => ({ key, message: translate('default_save_field_error_message') }));

    setErrors(keyBy(errors, 'key'));

    if (errors.length === 0) {
      props.onClose();
    }
  };

  const formBody = (
    <form id={FORM_ID} onSubmit={handleSubmit}>
      <Spinner ariaLabel={translate('settings.authentication.form.loading')} isLoading={loading}>
        <FlagMessage className="sw-w-full sw-mb-8" variant="info">
          <span>
            <FormattedMessage
              id="settings.authentication.help"
              values={{
                link: (
                  <DocumentationLink to={AlmAuthDocLinkKeys[tab]}>
                    {translate('settings.authentication.help.link')}
                  </DocumentationLink>
                ),
              }}
            />
          </span>
        </FlagMessage>

        {Object.values(values).map((val) => {
          if (excludedField.includes(val.key)) {
            return null;
          }

          return (
            <div key={val.key} className="sw-mb-8">
              <AuthenticationFormField
                definition={val.definition}
                error={errors[val.key]?.message}
                isNotSet={val.isNotSet}
                mandatory={val.mandatory}
                onFieldChange={setNewValue}
                settingValue={values[val.key]?.newValue ?? values[val.key]?.value}
              />
            </div>
          );
        })}
      </Spinner>
    </form>
  );

  return (
    <Modal
      body={formBody}
      headerTitle={header}
      isScrollable
      onClose={props.onClose}
      primaryButton={
        <Button
          form={FORM_ID}
          type="submit"
          hasAutoFocus
          isDisabled={!canBeSave}
          variety={ButtonVariety.Primary}
        >
          {translate('settings.almintegration.form.save')}

          <Spinner className="sw-ml-2" isLoading={loading} />
        </Button>
      }
    />
  );
}
