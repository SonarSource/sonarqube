/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import Input from './inputs/Input';
import DefinitionDefaults from './DefinitionDefaults';
import DefinitionChanges from './DefinitionChanges';
import {
  getPropertyName,
  getPropertyDescription,
  getSettingValue,
  isDefaultOrInherited
} from '../utils';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { resetValue, saveValue } from '../store/actions';
import { passValidation } from '../store/settingsPage/validationMessages/actions';
import { cancelChange, changeValue } from '../store/settingsPage/changedValues/actions';
import { TYPE_PASSWORD } from '../constants';
import {
  getSettingsAppChangedValue,
  isSettingsAppLoading,
  getSettingsAppValidationMessage
} from '../../../store/rootReducer';

class Definition extends React.PureComponent {
  mounted: boolean;
  timeout: number;

  static propTypes = {
    component: React.PropTypes.object,
    setting: React.PropTypes.object.isRequired,
    changedValue: React.PropTypes.any,
    loading: React.PropTypes.bool.isRequired,
    validationMessage: React.PropTypes.string,

    changeValue: React.PropTypes.func.isRequired,
    cancelChange: React.PropTypes.func.isRequired,
    saveValue: React.PropTypes.func.isRequired,
    resetValue: React.PropTypes.func.isRequired,
    passValidation: React.PropTypes.func.isRequired
  };

  state = {
    success: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  safeSetState(changes) {
    if (this.mounted) {
      this.setState(changes);
    }
  }

  handleChange(value) {
    clearTimeout(this.timeout);
    this.props.changeValue(this.props.setting.definition.key, value);
    if (this.props.setting.definition.type === TYPE_PASSWORD) {
      this.handleSave();
    }
  }

  handleReset() {
    const componentKey = this.props.component ? this.props.component.key : null;
    const { definition } = this.props.setting;
    return this.props
      .resetValue(definition.key, componentKey)
      .then(() => {
        this.safeSetState({ success: true });
        this.timeout = setTimeout(() => this.safeSetState({ success: false }), 3000);
      })
      .catch(() => {
        /* do nothing */
      });
  }

  handleCancel() {
    this.props.cancelChange(this.props.setting.definition.key);
    this.props.passValidation(this.props.setting.definition.key);
  }

  handleSave() {
    this.safeSetState({ success: false });
    const componentKey = this.props.component ? this.props.component.key : null;
    this.props
      .saveValue(this.props.setting.definition.key, componentKey)
      .then(() => {
        this.safeSetState({ success: true });
        this.timeout = setTimeout(() => this.safeSetState({ success: false }), 3000);
      })
      .catch(() => {
        /* do nothing */
      });
  }

  render() {
    const { setting, changedValue, loading } = this.props;
    const { definition } = setting;
    const propertyName = getPropertyName(definition);

    const hasValueChanged = changedValue != null;

    const className = classNames('settings-definition', {
      'settings-definition-changed': hasValueChanged
    });

    const effectiveValue = hasValueChanged ? changedValue : getSettingValue(setting);

    const isDefault = isDefaultOrInherited(setting) && !hasValueChanged;

    return (
      <div className={className} data-key={definition.key}>
        <div className="settings-definition-left">
          <h3 className="settings-definition-name" title={propertyName}>
            {propertyName}
          </h3>

          <div
            className="markdown small spacer-top"
            dangerouslySetInnerHTML={{ __html: getPropertyDescription(definition) }}
          />

          <div className="settings-definition-key note little-spacer-top">
            {translateWithParameters('settings.key_x', definition.key)}
          </div>
        </div>

        <div className="settings-definition-right">
          <Input setting={setting} value={effectiveValue} onChange={this.handleChange.bind(this)} />

          {!hasValueChanged &&
            <DefinitionDefaults
              setting={setting}
              isDefault={isDefault}
              onReset={() => this.handleReset()}
            />}

          {hasValueChanged &&
            <DefinitionChanges
              onSave={this.handleSave.bind(this)}
              onCancel={this.handleCancel.bind(this)}
            />}

          <div className="settings-definition-state">
            {loading &&
              <span className="text-info">
                <span className="settings-definition-state-icon">
                  <i className="spinner" />
                </span>
                {translate('settings.state.saving')}
              </span>}

            {!loading &&
              this.props.validationMessage != null &&
              <span className="text-danger">
                <span className="settings-definition-state-icon">
                  <i className="icon-alert-error" />
                </span>
                {translateWithParameters(
                  'settings.state.validation_failed',
                  this.props.validationMessage
                )}
              </span>}

            {!loading &&
              this.state.success &&
              <span className="text-success">
                <span className="settings-definition-state-icon">
                  <i className="icon-check" />
                </span>
                {translate('settings.state.saved')}
              </span>}
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  changedValue: getSettingsAppChangedValue(state, ownProps.setting.definition.key),
  loading: isSettingsAppLoading(state, ownProps.setting.definition.key),
  validationMessage: getSettingsAppValidationMessage(state, ownProps.setting.definition.key)
});

export default connect(mapStateToProps, {
  changeValue,
  saveValue,
  resetValue,
  passValidation,
  cancelChange
})(Definition);
