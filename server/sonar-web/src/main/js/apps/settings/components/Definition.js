/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { connect } from 'react-redux';
import shallowCompare from 'react-addons-shallow-compare';
import Input from './inputs/Input';
import DefinitionDefaults from './DefinitionDefaults';
import { getPropertyName, getPropertyDescription, isEmptyValue } from '../utils';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { setValue, resetValue } from '../store/actions';
import { isLoading, getValidationMessage } from '../store/rootReducer';
import { failValidation } from '../store/settingsPage/actions';

class Definition extends React.Component {
  static propTypes = {
    component: React.PropTypes.object,
    setting: React.PropTypes.object.isRequired,
    loading: React.PropTypes.bool.isRequired,
    validationMessage: React.PropTypes.string,
    setValue: React.PropTypes.func.isRequired,
    resetValue: React.PropTypes.func.isRequired,
    failValidation: React.PropTypes.func.isRequired
  };

  state = {
    success: false
  };

  componentDidMount () {
    this.mounted = true;
  }

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  componentWillUpdate (nextProps) {
    if (nextProps.validationMessage != null) {
      this.safeSetState({ success: false });
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  safeSetState (changes) {
    if (this.mounted) {
      this.setState(changes);
    }
  }

  handleSet (_, value) {
    clearTimeout(this.timeout);
    const componentKey = this.props.component ? this.props.component.key : null;
    const { definition } = this.props.setting;

    if (isEmptyValue(definition, value)) {
      this.props.failValidation(definition.key, translate('settings.state.value_cant_be_empty'));
      return;
    }

    return this.props.setValue(definition, value, componentKey).then(() => {
      this.safeSetState({ success: true });
      this.timeout = setTimeout(() => this.safeSetState({ success: false }), 3000);
    });
  }

  handleReset () {
    this.handleSet(null, null);
  }

  render () {
    const { setting, loading } = this.props;
    const { definition } = setting;
    const propertyName = getPropertyName(definition);

    return (
        <div className="settings-definition" data-key={definition.key}>
          <div className="settings-definition-left">
            <h3 className="settings-definition-name" title={propertyName}>
              {propertyName}
            </h3>

            <div className="settings-definition-description markdown small spacer-top"
                 dangerouslySetInnerHTML={{ __html: getPropertyDescription(definition) }}/>

            <div className="settings-definition-key note little-spacer-top">
              {translateWithParameters('settings.key_x', definition.key)}
            </div>
          </div>

          <div className="settings-definition-right">
            <Input setting={setting} onChange={this.handleSet.bind(this)}/>

            <DefinitionDefaults setting={setting} onReset={() => this.handleReset()}/>

            <div className="settings-definition-state">
              {loading && (
                  <span className="text-info">
                    <span className="settings-definition-state-icon">
                      <i className="spinner"/>
                    </span>
                    {translate('settings.state.saving')}
                  </span>
              )}

              {!loading && (this.props.validationMessage != null) && (
                  <span className="text-danger">
                    <span className="settings-definition-state-icon">
                      <i className="icon-alert-error"/>
                    </span>
                    {translateWithParameters('settings.state.validation_failed', this.props.validationMessage)}
                  </span>
              )}

              {!loading && this.state.success && (
                  <span className="text-success">
                    <span className="settings-definition-state-icon">
                      <i className="icon-check"/>
                    </span>
                    {translate('settings.state.saved')}
                  </span>
              )}
            </div>
          </div>
        </div>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  loading: isLoading(state, ownProps.setting.definition.key),
  validationMessage: getValidationMessage(state, ownProps.setting.definition.key)
});

export default connect(
    mapStateToProps,
    { setValue, resetValue, failValidation }
)(Definition);
