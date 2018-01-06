/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import PropTypes from 'prop-types';
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
  /*:: mounted: boolean; */
  /*:: timeout: number; */

  static propTypes = {
    component: PropTypes.object,
    setting: PropTypes.object.isRequired,
    changedValue: PropTypes.any,
    loading: PropTypes.bool.isRequired,
    validationMessage: PropTypes.string,

    changeValue: PropTypes.func.isRequired,
    cancelChange: PropTypes.func.isRequired,
    saveValue: PropTypes.func.isRequired,
    resetValue: PropTypes.func.isRequired,
    passValidation: PropTypes.func.isRequired
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

  handleChange = value => {
    clearTimeout(this.timeout);
    this.props.changeValue(this.props.setting.definition.key, value);
    if (this.props.setting.definition.type === TYPE_PASSWORD) {
      this.handleSave();
    }
  };

  handleReset = () => {
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
  };

  handleCancel = () => {
    const componentKey = this.props.component ? this.props.component.key : null;
    this.props.cancelChange(this.props.setting.definition.key, componentKey);
    this.props.passValidation(this.props.setting.definition.key);
  };

  handleSave = () => {
    if (this.props.changedValue != null) {
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
  };

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
          <div className="settings-definition-state">
            {loading && (
              <span className="text-info">
                <i className="spinner spacer-right" />
                {translate('settings.state.saving')}
              </span>
            )}

            {!loading &&
              this.props.validationMessage != null && (
                <span className="text-danger">
                  <i className="icon-alert-error spacer-right" />
                  <span>
                    {translateWithParameters(
                      'settings.state.validation_failed',
                      this.props.validationMessage
                    )}
                  </span>
                </span>
              )}

            {!loading &&
              this.state.success && (
                <span className="text-success">
                  <i className="icon-check spacer-right" />
                  {translate('settings.state.saved')}
                </span>
              )}
          </div>

          <Input
            setting={setting}
            value={effectiveValue}
            onCancel={this.handleCancel}
            onChange={this.handleChange}
            onSave={this.handleSave}
          />

          {!hasValueChanged && (
            <DefinitionDefaults
              setting={setting}
              isDefault={isDefault}
              onReset={this.handleReset}
            />
          )}

          {hasValueChanged && (
            <DefinitionChanges onSave={this.handleSave} onCancel={this.handleCancel} />
          )}
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
