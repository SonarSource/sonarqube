/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { connect } from 'react-redux';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import {
  getSettingsAppChangedValue,
  getSettingsAppValidationMessage,
  isSettingsAppLoading,
  Store
} from '../../../store/rootReducer';
import { checkValue, resetValue, saveValue } from '../store/actions';
import { cancelChange, changeValue, passValidation } from '../store/settingsPage';
import {
  getPropertyDescription,
  getPropertyName,
  getSettingValue,
  isDefaultOrInherited,
  sanitizeTranslation
} from '../utils';
import DefinitionActions from './DefinitionActions';
import Input from './inputs/Input';

interface Props {
  cancelChange: (key: string) => void;
  changeValue: (key: string, value: any) => void;
  changedValue: any;
  checkValue: (key: string) => boolean;
  component?: T.Component;
  loading: boolean;
  passValidation: (key: string) => void;
  resetValue: (key: string, component?: string) => Promise<void>;
  saveValue: (key: string, component?: string) => Promise<void>;
  setting: T.Setting;
  validationMessage?: string;
}

interface State {
  success: boolean;
}

export class Definition extends React.PureComponent<Props, State> {
  timeout?: number;
  mounted = false;
  state = { success: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  safeSetState(changes: State) {
    if (this.mounted) {
      this.setState(changes);
    }
  }

  handleChange = (value: any) => {
    clearTimeout(this.timeout);
    this.props.changeValue(this.props.setting.definition.key, value);
    this.handleCheck();
  };

  handleReset = () => {
    const { component, setting } = this.props;
    const { definition } = setting;
    const componentKey = component && component.key;
    return this.props.resetValue(definition.key, componentKey).then(() => {
      this.props.cancelChange(definition.key);
      this.safeSetState({ success: true });
      this.timeout = window.setTimeout(() => this.safeSetState({ success: false }), 3000);
    });
  };

  handleCancel = () => {
    const { setting } = this.props;
    this.props.cancelChange(setting.definition.key);
    this.props.passValidation(setting.definition.key);
  };

  handleCheck = () => {
    const { setting } = this.props;
    this.props.checkValue(setting.definition.key);
  };

  handleSave = () => {
    if (this.props.changedValue != null) {
      this.safeSetState({ success: false });
      const { component, setting } = this.props;
      this.props.saveValue(setting.definition.key, component && component.key).then(
        () => {
          this.safeSetState({ success: true });
          this.timeout = window.setTimeout(() => this.safeSetState({ success: false }), 3000);
        },
        () => {}
      );
    }
  };

  render() {
    const { changedValue, loading, setting, validationMessage } = this.props;
    const { definition } = setting;
    const propertyName = getPropertyName(definition);
    const hasError = validationMessage != null;
    const hasValueChanged = changedValue != null;
    const effectiveValue = hasValueChanged ? changedValue : getSettingValue(setting);
    const isDefault = isDefaultOrInherited(setting);
    const description = getPropertyDescription(definition);
    return (
      <div
        className={classNames('settings-definition', {
          'settings-definition-changed': hasValueChanged
        })}
        data-key={definition.key}>
        <div className="settings-definition-left">
          <h3 className="settings-definition-name" title={propertyName}>
            {propertyName}
          </h3>

          {description && (
            <div
              className="markdown small spacer-top"
              dangerouslySetInnerHTML={{ __html: sanitizeTranslation(description) }}
            />
          )}

          <div className="settings-definition-key note little-spacer-top">
            {translateWithParameters('settings.key_x', definition.key)}
          </div>
        </div>

        <div className="settings-definition-right">
          <div className="settings-definition-state">
            {loading && (
              <span className="text-info">
                <i className="spinner spacer-right" />
                {translate('settings.state.saving')}
              </span>
            )}

            {!loading && validationMessage && (
              <span className="text-danger">
                <AlertErrorIcon className="spacer-right" />
                <span>
                  {translateWithParameters('settings.state.validation_failed', validationMessage)}
                </span>
              </span>
            )}

            {!loading && !hasError && this.state.success && (
              <span className="text-success">
                <AlertSuccessIcon className="spacer-right" />
                {translate('settings.state.saved')}
              </span>
            )}
          </div>

          <Input
            hasValueChanged={hasValueChanged}
            onCancel={this.handleCancel}
            onChange={this.handleChange}
            onSave={this.handleSave}
            setting={setting}
            value={effectiveValue}
          />

          <DefinitionActions
            changedValue={changedValue}
            hasError={hasError}
            hasValueChanged={hasValueChanged}
            isDefault={isDefault}
            onCancel={this.handleCancel}
            onReset={this.handleReset}
            onSave={this.handleSave}
            setting={setting}
          />
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: Store, ownProps: Pick<Props, 'setting'>) => ({
  changedValue: getSettingsAppChangedValue(state, ownProps.setting.definition.key),
  loading: isSettingsAppLoading(state, ownProps.setting.definition.key),
  validationMessage: getSettingsAppValidationMessage(state, ownProps.setting.definition.key)
});

const mapDispatchToProps = {
  cancelChange: cancelChange as any,
  changeValue: changeValue as any,
  checkValue: checkValue as any,
  passValidation: passValidation as any,
  resetValue: resetValue as any,
  saveValue: saveValue as any
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Definition);
