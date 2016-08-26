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
import DefinitionInput from './inputs/Input';
import { getPropertyName, getPropertyDescription } from '../utils';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { setValue } from '../store/actions';
import { isLoading, getValidationMessage } from '../store/rootReducer';

class Definition extends React.Component {
  static propTypes = {
    component: React.PropTypes.object,
    setting: React.PropTypes.object.isRequired,
    loading: React.PropTypes.bool.isRequired,
    validationMessage: React.PropTypes.string,
    setValue: React.PropTypes.func.isRequired
  };

  state = {
    success: false
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  componentWillUpdate (nextProps) {
    if (nextProps.validationMessage != null) {
      this.setState({ success: false });
    }
  }

  handleSet (_, value) {
    clearTimeout(this.timeout);
    const componentKey = this.props.component ? this.props.component.key : null;
    return this.props.setValue(this.props.setting.definition.key, value, componentKey).then(() => {
      this.setState({ success: true });
      this.timeout = setTimeout(() => this.setState({ success: false }), 3000);
    });
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
            <DefinitionInput setting={setting} onChange={this.handleSet.bind(this)}/>

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
    { setValue }
)(Definition);
