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
import { ButtonPrimary, FlagMessage, Modal, Spinner } from 'design-system';
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
  create: boolean;
  loading: boolean;
  values: Dict<SettingValue>;
  setNewValue: (key: string, value: string | boolean) => void;
  canBeSave: boolean;
  onClose: () => void;
  tab: AuthenticationTabs;
  excludedField: string[];
  hasLegacyConfiguration?: boolean;
  provisioningStatus?: ProvisioningType;
}

interface ErrorValue {
  key: string;
  message: string;
}

export default function ConfigurationForm(props: Readonly<Props>) {
  const {
    create,
    loading,
    values,
    setNewValue,
    canBeSave,
    tab,
    excludedField,
    hasLegacyConfiguration,
    provisioningStatus,
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
      <Spinner loading={loading} ariaLabel={translate('settings.authentication.form.loading')}>
        <FlagMessage
          className="sw-w-full sw-mb-8"
          variant={hasLegacyConfiguration ? 'warning' : 'info'}
        >
          <span>
            <FormattedMessage
              id={`settings.authentication.${helpMessage}`}
              defaultMessage={translate(`settings.authentication.${helpMessage}`)}
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
                settingValue={values[val.key]?.newValue ?? values[val.key]?.value}
                definition={val.definition}
                mandatory={val.mandatory}
                onFieldChange={setNewValue}
                isNotSet={!isSet}
                error={errors[val.key]?.message}
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
        headerTitle={header}
        isScrollable
        onClose={props.onClose}
        body={formBody}
        primaryButton={
          <ButtonPrimary form={FORM_ID} type="submit" autoFocus disabled={!canBeSave}>
            {translate('settings.almintegration.form.save')}
            <Spinner className="sw-ml-2" loading={loading} />
          </ButtonPrimary>
        }
      />
      {showConfirmModal && (
        <GitHubConfirmModal
          onConfirm={onSave}
          onClose={() => setShowConfirmModal(false)}
          values={values}
          provisioningStatus={provisioningStatus ?? ProvisioningType.jit}
        />
      )}
    </>
  );
}
