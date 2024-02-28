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

import { Spinner } from '@sonarsource/echoes-react';
import { ButtonPrimary, FlagMessage, Modal } from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { translate } from '../../../../helpers/l10n';
import { useSaveValuesMutation } from '../../../../queries/settings';
import { AlmKeys } from '../../../../types/alm-settings';
import { ProvisioningType } from '../../../../types/provisioning';
import { Dict } from '../../../../types/types';
import { AuthenticationTabs, DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import GitHubConfirmModal from './GitHubConfirmModal';
import { SettingValue } from './hook/useConfiguration';
import { isAllowToSignUpEnabled, isOrganizationListEmpty } from './hook/useGithubConfiguration';

interface Props {
  canBeSave: boolean;
  create: boolean;
  excludedField: string[];
  hasLegacyConfiguration?: boolean;
  loading: boolean;
  onClose: () => void;
  provisioningStatus?: ProvisioningType;
  setNewValue: (key: string, value: string | boolean) => void;
  tab: AuthenticationTabs;
  values: Dict<SettingValue>;
}

interface ErrorValue {
  key: string;
  message: string;
}

export default function ConfigurationForm(props: Readonly<Props>) {
  const {
    canBeSave,
    create,
    excludedField,
    hasLegacyConfiguration,
    loading,
    provisioningStatus,
    setNewValue,
    tab,
    values,
  } = props;
  const [errors, setErrors] = React.useState<Dict<ErrorValue>>({});
  const [showConfirmModal, setShowConfirmModal] = React.useState(false);

  const { mutateAsync: changeConfig } = useSaveValuesMutation();

  const header = translate('settings.authentication.form', create ? 'create' : 'edit', tab);

  const handleSubmit = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (canBeSave) {
      if (
        tab === AlmKeys.GitHub &&
        isOrganizationListEmpty(values) &&
        (provisioningStatus === ProvisioningType.auto || isAllowToSignUpEnabled(values))
      ) {
        setShowConfirmModal(true);
      } else {
        await onSave();
      }
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

  const helpMessage = hasLegacyConfiguration ? `legacy_help.${tab}` : 'help';

  const FORM_ID = 'configuration-form';

  const formBody = (
    <form id={FORM_ID} onSubmit={handleSubmit}>
      <Spinner ariaLabel={translate('settings.authentication.form.loading')} isLoading={loading}>
        <FlagMessage
          className="sw-w-full sw-mb-8"
          variant={hasLegacyConfiguration ? 'warning' : 'info'}
        >
          <span>
            <FormattedMessage
              defaultMessage={translate(`settings.authentication.${helpMessage}`)}
              id={`settings.authentication.${helpMessage}`}
              values={{
                link: (
                  <DocumentationLink
                    to={`/instance-administration/authentication/${DOCUMENTATION_LINK_SUFFIXES[tab]}/`}
                  >
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

          const isSet = hasLegacyConfiguration ? false : !val.isNotSet;

          return (
            <div key={val.key} className="sw-mb-8">
              <AuthenticationFormField
                definition={val.definition}
                error={errors[val.key]?.message}
                isNotSet={!isSet}
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
    <>
      <Modal
        body={formBody}
        headerTitle={header}
        isScrollable
        onClose={props.onClose}
        primaryButton={
          <ButtonPrimary form={FORM_ID} type="submit" autoFocus disabled={!canBeSave}>
            {translate('settings.almintegration.form.save')}

            <Spinner className="sw-ml-2" isLoading={loading} />
          </ButtonPrimary>
        }
      />
      {showConfirmModal && (
        <GitHubConfirmModal
          onClose={() => setShowConfirmModal(false)}
          onConfirm={onSave}
          provisioningStatus={provisioningStatus ?? ProvisioningType.jit}
          values={values}
        />
      )}
    </>
  );
}
