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
import classNames from 'classnames';
import { keyBy } from 'lodash';
import React from 'react';
import { getValues, resetSettingValue, setSettingValue } from '../../../../api/settings';
import { SubmitButton } from '../../../../components/controls/buttons';
import Tooltip from '../../../../components/controls/Tooltip';
import { Location, withRouter } from '../../../../components/hoc/withRouter';
import AlertSuccessIcon from '../../../../components/icons/AlertSuccessIcon';
import AlertWarnIcon from '../../../../components/icons/AlertWarnIcon';
import DetachIcon from '../../../../components/icons/DetachIcon';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isSuccessStatus, parseError } from '../../../../helpers/request';
import { getBaseUrl } from '../../../../helpers/system';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../../types/settings';
import SamlFormField from './SamlFormField';
import SamlToggleField from './SamlToggleField';

interface SamlAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
  location: Location;
}

interface SamlAuthenticationState {
  settingValue: Pick<SettingValue, 'key' | 'value'>[];
  submitting: boolean;
  dirtyFields: string[];
  securedFieldsSubmitted: string[];
  error: { [key: string]: string };
  success?: boolean;
}

const CONFIG_TEST_PATH = '/saml/validation_init';

const SAML_ENABLED_FIELD = 'sonar.auth.saml.enabled';

const OPTIONAL_FIELDS = [
  'sonar.auth.saml.sp.certificate.secured',
  'sonar.auth.saml.sp.privateKey.secured',
  'sonar.auth.saml.signature.enabled',
  'sonar.auth.saml.user.email',
  'sonar.auth.saml.group.name',
  'sonar.scim.enabled',
];

class SamlAuthentication extends React.PureComponent<
  SamlAuthenticationProps,
  SamlAuthenticationState
> {
  formFieldRef: React.RefObject<HTMLDivElement> = React.createRef();

  constructor(props: SamlAuthenticationProps) {
    super(props);
    const settingValue = props.definitions.map((def) => {
      return {
        key: def.key,
      };
    });

    this.state = {
      settingValue,
      submitting: false,
      dirtyFields: [],
      securedFieldsSubmitted: [],
      error: {},
    };
  }

  componentDidMount() {
    const { definitions } = this.props;
    const keys = definitions.map((definition) => definition.key);
    // Added setTimeout to make sure the component gets updated before scrolling
    setTimeout(() => {
      if (location.hash) {
        this.scrollToSearchedField();
      }
    });
    this.loadSettingValues(keys);
  }

  componentDidUpdate(prevProps: SamlAuthenticationProps) {
    const { location } = this.props;
    if (prevProps.location.hash !== location.hash) {
      this.scrollToSearchedField();
    }
  }

  scrollToSearchedField = () => {
    if (this.formFieldRef.current) {
      this.formFieldRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'nearest',
      });
    }
  };

  onFieldChange = (id: string, value: string | boolean) => {
    const { settingValue, dirtyFields } = this.state;
    const updatedSettingValue = settingValue?.map((set) => {
      if (set.key === id) {
        set.value = String(value);
      }
      return set;
    });

    if (!dirtyFields.includes(id)) {
      const updatedDirtyFields = [...dirtyFields, id];
      this.setState({
        dirtyFields: updatedDirtyFields,
      });
    }

    this.setState({
      settingValue: updatedSettingValue,
    });
  };

  async loadSettingValues(keys: string[]) {
    const { settingValue, securedFieldsSubmitted } = this.state;
    const values = await getValues({
      keys,
    });
    const valuesByDefinitionKey = keyBy(values, 'key');
    const updatedSecuredFieldsSubmitted: string[] = [...securedFieldsSubmitted];
    const updateSettingValue = settingValue?.map((set) => {
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
      securedFieldsSubmitted: updatedSecuredFieldsSubmitted,
    });
  }

  isSecuredField = (key: string) => {
    const { definitions } = this.props;
    const fieldDefinition = definitions.find((def) => def.key === key);
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

    this.setState({ submitting: true, error: {}, success: false });
    const promises: Promise<void>[] = [];

    dirtyFields.forEach((field) => {
      const definition = definitions.find((def) => def.key === field);
      const value = settingValue.find((def) => def.key === field)?.value;
      if (definition && value !== undefined) {
        const apiCall =
          value.length > 0
            ? setSettingValue(definition, value)
            : resetSettingValue({ keys: definition.key });

        promises.push(apiCall);
      }
    });

    await Promise.all(promises.map((p) => p.catch((e) => e))).then((data) => {
      const dataWithError = data
        .map((data, index) => ({ data, index }))
        .filter((d) => d.data !== undefined && !isSuccessStatus(d.data.status));
      if (dataWithError.length > 0) {
        dataWithError.forEach(async (d) => {
          const validationMessage = await parseError(d.data as Response);
          const { error } = this.state;
          this.setState({
            error: { ...error, ...{ [dirtyFields[d.index]]: validationMessage } },
          });
        });
      }
      this.setState({ success: dirtyFields.length !== dataWithError.length });
    });
    await this.loadSettingValues(dirtyFields);
    this.setState({ submitting: false, dirtyFields: [] });
  };

  allowEnabling = () => {
    const { settingValue } = this.state;
    const enabledFlagSettingValue = settingValue.find((set) => set.key === SAML_ENABLED_FIELD);

    if (enabledFlagSettingValue && enabledFlagSettingValue.value === 'true') {
      return true;
    }

    return this.getEmptyRequiredFields().length === 0;
  };

  onEnableFlagChange = (value: boolean) => {
    const { settingValue, dirtyFields } = this.state;

    const updatedSettingValue = settingValue?.map((set) => {
      if (set.key === SAML_ENABLED_FIELD) {
        set.value = String(value);
      }
      return set;
    });

    this.setState(
      {
        settingValue: updatedSettingValue,
        dirtyFields: [...dirtyFields, SAML_ENABLED_FIELD],
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

  getEmptyRequiredFields = () => {
    const { settingValue, securedFieldsSubmitted } = this.state;
    const { definitions } = this.props;

    const updatedRequiredFields: string[] = [];

    for (const setting of settingValue) {
      const isMandatory = !OPTIONAL_FIELDS.includes(setting.key);
      const isSecured = this.isSecuredField(setting.key);
      const isSecuredAndNotSubmitted = isSecured && !securedFieldsSubmitted.includes(setting.key);
      const isNotSecuredAndNotSubmitted =
        !isSecured && (setting.value === '' || setting.value === undefined);
      if (isMandatory && (isSecuredAndNotSubmitted || isNotSecuredAndNotSubmitted)) {
        const settingDef = definitions.find((def) => def.key === setting.key);

        if (settingDef && settingDef.name) {
          updatedRequiredFields.push(settingDef.name);
        }
      }
    }
    return updatedRequiredFields;
  };

  render() {
    const { definitions } = this.props;
    const { submitting, settingValue, securedFieldsSubmitted, error, dirtyFields, success } =
      this.state;
    const enabledFlagDefinition = definitions.find((def) => def.key === SAML_ENABLED_FIELD);

    const formIsIncomplete = !this.allowEnabling();
    const preventTestingConfig = this.getEmptyRequiredFields().length > 0 || dirtyFields.length > 0;

    return (
      <div>
        {definitions.map((def) => {
          if (def.key === SAML_ENABLED_FIELD) {
            return null;
          }
          return (
            <div
              key={def.key}
              ref={this.props.location.hash.substring(1) === def.key ? this.formFieldRef : null}
            >
              <SamlFormField
                settingValue={settingValue?.find((set) => set.key === def.key)}
                definition={def}
                mandatory={!OPTIONAL_FIELDS.includes(def.key)}
                onFieldChange={this.onFieldChange}
                showSecuredTextArea={
                  !securedFieldsSubmitted.includes(def.key) || dirtyFields.includes(def.key)
                }
                error={error}
              />
            </div>
          );
        })}
        <div className="fixed-footer padded">
          {enabledFlagDefinition && (
            <Tooltip
              overlay={
                this.allowEnabling()
                  ? null
                  : translateWithParameters(
                      'settings.authentication.saml.tooltip.required_fields',
                      this.getEmptyRequiredFields().join(', ')
                    )
              }
            >
              <div className="display-inline-flex-center">
                <label className="h3 spacer-right">{enabledFlagDefinition.name}</label>
                <SamlToggleField
                  definition={enabledFlagDefinition}
                  settingValue={settingValue?.find((set) => set.key === enabledFlagDefinition.key)}
                  toggleDisabled={formIsIncomplete}
                  onChange={this.onEnableFlagChange}
                />
              </div>
            </Tooltip>
          )}
          <div className="display-inline-flex-center">
            {success && (
              <div className="spacer-right">
                <Tooltip
                  overlay={
                    Object.keys(error).length > 0
                      ? translateWithParameters(
                          'settings.authentication.saml.form.save_warn',
                          Object.keys(error).length
                        )
                      : null
                  }
                >
                  {Object.keys(error).length > 0 ? (
                    <span>
                      <AlertWarnIcon className="spacer-right" />
                      {translate('settings.authentication.saml.form.save_partial')}
                    </span>
                  ) : (
                    <span>
                      <AlertSuccessIcon className="spacer-right" />
                      {translate('settings.authentication.saml.form.save_success')}
                    </span>
                  )}
                  {}
                </Tooltip>
              </div>
            )}
            <SubmitButton className="button-primary spacer-right" onClick={this.onSaveConfig}>
              {translate('settings.authentication.saml.form.save')}
              <DeferredSpinner className="spacer-left" loading={submitting} />
            </SubmitButton>

            <Tooltip
              overlay={this.getTestButtonTooltipContent(formIsIncomplete, dirtyFields.length > 0)}
            >
              <a
                className={classNames('button', {
                  disabled: preventTestingConfig,
                })}
                href={preventTestingConfig ? undefined : `${getBaseUrl()}${CONFIG_TEST_PATH}`}
                target="_blank"
                rel="noopener noreferrer"
              >
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

export default withRouter(SamlAuthentication);
