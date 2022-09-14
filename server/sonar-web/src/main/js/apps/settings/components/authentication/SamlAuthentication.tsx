/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import { keyBy } from 'lodash';
import React from 'react';
import { getValues, resetSettingValue, setSettingValue } from '../../../../api/settings';
import { SubmitButton } from '../../../../components/controls/buttons';
import Tooltip from '../../../../components/controls/Tooltip';
import DetachIcon from '../../../../components/icons/DetachIcon';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate } from '../../../../helpers/l10n';
import { parseError } from '../../../../helpers/request';
import { getBaseUrl } from '../../../../helpers/system';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../../types/settings';
import SamlFormField from './SamlFormField';
import SamlToggleField from './SamlToggleField';

interface SamlAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
}

interface SamlAuthenticationState {
  settingValue: Pick<SettingValue, 'key' | 'value'>[];
  submitting: boolean;
  dirtyFields: string[];
  securedFieldsSubmitted: string[];
  error: { [key: string]: string };
}

const CONFIG_TEST_PATH = '/api/saml/validation_init';

const SAML_ENABLED_FIELD = 'sonar.auth.saml.enabled';

const OPTIONAL_FIELDS = [
  'sonar.auth.saml.sp.certificate.secured',
  'sonar.auth.saml.sp.privateKey.secured',
  'sonar.auth.saml.signature.enabled',
  'sonar.auth.saml.user.email',
  'sonar.auth.saml.group.name'
];

class SamlAuthentication extends React.PureComponent<
  SamlAuthenticationProps,
  SamlAuthenticationState
> {
  constructor(props: SamlAuthenticationProps) {
    super(props);
    const settingValue = props.definitions.map(def => {
      return {
        key: def.key
      };
    });

    this.state = {
      settingValue,
      submitting: false,
      dirtyFields: [],
      securedFieldsSubmitted: [],
      error: {}
    };
  }

  componentDidMount() {
    const { definitions } = this.props;
    const keys = definitions.map(definition => definition.key).join(',');
    this.loadSettingValues(keys);
  }

  onFieldChange = (id: string, value: string | boolean) => {
    const { settingValue, dirtyFields } = this.state;
    const updatedSettingValue = settingValue?.map(set => {
      if (set.key === id) {
        set.value = String(value);
      }
      return set;
    });

    if (!dirtyFields.includes(id)) {
      const updatedDirtyFields = [...dirtyFields, id];
      this.setState({
        dirtyFields: updatedDirtyFields
      });
    }

    this.setState({
      settingValue: updatedSettingValue
    });
  };

  async loadSettingValues(keys: string) {
    const { settingValue, securedFieldsSubmitted } = this.state;
    const values = await getValues({
      keys
    });
    const valuesByDefinitionKey = keyBy(values, 'key');
    const updatedSecuredFieldsSubmitted: string[] = [...securedFieldsSubmitted];
    const updateSettingValue = settingValue?.map(set => {
      if (valuesByDefinitionKey[set.key]) {
        set.value =
          valuesByDefinitionKey[set.key].value ?? valuesByDefinitionKey[set.key].parentValue;
      }

      if (
        this.isSecuredField(set.key) &&
        valuesByDefinitionKey[set.key] &&
        !securedFieldsSubmitted.includes(set.key)
      ) {
        updatedSecuredFieldsSubmitted.push(set.key);
      }

      return set;
    });

    this.setState({
      settingValue: updateSettingValue,
      securedFieldsSubmitted: updatedSecuredFieldsSubmitted
    });
  }

  isSecuredField = (key: string) => {
    const { definitions } = this.props;
    const fieldDefinition = definitions.find(def => def.key === key);
    if (fieldDefinition && fieldDefinition.type === SettingType.PASSWORD) {
      return true;
    }
    return false;
  };

  onSaveConfig = async () => {
    const { settingValue, dirtyFields } = this.state;
    const { definitions } = this.props;

    if (dirtyFields.length === 0) {
      return;
    }

    this.setState({ submitting: true, error: {} });
    const promises: Promise<void>[] = [];

    settingValue?.forEach(set => {
      const definition = definitions.find(def => def.key === set.key);
      if (definition && set.value !== undefined && dirtyFields.includes(set.key)) {
        const apiCall =
          set.value.length > 0
            ? setSettingValue(definition, set.value)
            : resetSettingValue({ keys: definition.key });
        const promise = apiCall.catch(async e => {
          const { error } = this.state;
          const validationMessage = await parseError(e as Response);
          this.setState({
            submitting: false,
            dirtyFields: [],
            error: { ...error, ...{ [set.key]: validationMessage } }
          });
        });
        promises.push(promise);
      }
    });
    await Promise.all(promises);
    await this.loadSettingValues(dirtyFields.join(','));

    this.setState({ submitting: false, dirtyFields: [] });
  };

  allowEnabling = () => {
    const { settingValue, securedFieldsSubmitted } = this.state;
    const enabledFlagSettingValue = settingValue.find(set => set.key === SAML_ENABLED_FIELD);
    if (enabledFlagSettingValue && enabledFlagSettingValue.value === 'true') {
      return true;
    }

    for (const setting of settingValue) {
      const isMandatory = !OPTIONAL_FIELDS.includes(setting.key);
      const isSecured = this.isSecuredField(setting.key);
      const isSecuredAndNotSubmitted = isSecured && !securedFieldsSubmitted.includes(setting.key);
      const isNotSecuredAndNotSubmitted =
        !isSecured && (setting.value === '' || setting.value === undefined);
      if (isMandatory && (isSecuredAndNotSubmitted || isNotSecuredAndNotSubmitted)) {
        return false;
      }
    }
    return true;
  };

  onEnableFlagChange = (value: boolean) => {
    const { settingValue, dirtyFields } = this.state;

    const updatedSettingValue = settingValue?.map(set => {
      if (set.key === SAML_ENABLED_FIELD) {
        set.value = String(value);
      }
      return set;
    });

    this.setState(
      {
        settingValue: updatedSettingValue,
        dirtyFields: [...dirtyFields, SAML_ENABLED_FIELD]
      },
      () => {
        this.onSaveConfig();
      }
    );
  };

  getTestButtonTooltipContent = (formIsIncomplete: boolean, hasDirtyFields: boolean) => {
    if (hasDirtyFields) {
      return translate('settings.authentication.saml.form.test.help.dirty');
    }

    if (formIsIncomplete) {
      return translate('settings.authentication.saml.form.test.help.incomplete');
    }

    return null;
  };

  render() {
    const { definitions } = this.props;
    const { submitting, settingValue, securedFieldsSubmitted, error, dirtyFields } = this.state;
    const enabledFlagDefinition = definitions.find(def => def.key === SAML_ENABLED_FIELD);

    const formIsIncomplete = !this.allowEnabling();
    const preventTestingConfig = formIsIncomplete || dirtyFields.length > 0;

    return (
      <div>
        {definitions.map(def => {
          if (def.key === SAML_ENABLED_FIELD) {
            return null;
          }
          return (
            <SamlFormField
              settingValue={settingValue?.find(set => set.key === def.key)}
              definition={def}
              mandatory={!OPTIONAL_FIELDS.includes(def.key)}
              onFieldChange={this.onFieldChange}
              showSecuredTextArea={
                !securedFieldsSubmitted.includes(def.key) || dirtyFields.includes(def.key)
              }
              error={error}
              key={def.key}
            />
          );
        })}
        <div className="fixed-footer padded-left padded-right">
          {enabledFlagDefinition && (
            <div>
              <label className="h3 spacer-right">{enabledFlagDefinition.name}</label>
              <SamlToggleField
                definition={enabledFlagDefinition}
                settingValue={settingValue?.find(set => set.key === enabledFlagDefinition.key)}
                toggleDisabled={formIsIncomplete}
                onChange={this.onEnableFlagChange}
              />
            </div>
          )}
          <div>
            <SubmitButton className="button-primary spacer-right" onClick={this.onSaveConfig}>
              {translate('settings.authentication.saml.form.save')}
              <DeferredSpinner className="spacer-left" loading={submitting} />
            </SubmitButton>

            <Tooltip
              overlay={this.getTestButtonTooltipContent(formIsIncomplete, dirtyFields.length > 0)}>
              <a
                className={classNames('button', {
                  disabled: preventTestingConfig
                })}
                href={preventTestingConfig ? undefined : `${getBaseUrl()}${CONFIG_TEST_PATH}`}
                target="_blank"
                rel="noopener noreferrer">
                <DetachIcon className="spacer-right" />
                {translate('settings.authentication.saml.form.test')}
              </a>
            </Tooltip>
          </div>
        </div>
      </div>
    );
  }
}

export default SamlAuthentication;
