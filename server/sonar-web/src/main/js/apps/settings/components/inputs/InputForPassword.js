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
import { translate } from '../../../../helpers/l10n';
import { defaultInputPropTypes } from '../../propTypes';

export default class InputForPassword extends React.Component {
  static propTypes = defaultInputPropTypes;

  state = {
    changing: false
  };

  handleChangeClick (e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ changing: true });
  }

  handleCancelChangeClick (e) {
    e.preventDefault();
    e.target.blur();
    this.setState({ changing: false });
  }

  handleFormSubmit (e) {
    e.preventDefault();
    const { value } = this.refs.input;
    this.props.onChange(undefined, value)
        .then(() => this.setState({ changing: false }));
  }

  renderInput () {
    return (
        <div>
          <form onSubmit={e => this.handleFormSubmit(e)}>
            <input className="hidden" type="password"/>
            <input
                ref="input"
                name={this.props.name}
                className="input-large text-top"
                type="password"
                autoFocus={true}
                autoComplete={false}/>
            <button className="spacer-left">{translate('set')}</button>
            <a className="spacer-left" href="#" onClick={e => this.handleCancelChangeClick(e)}>
              {translate('cancel')}
            </a>
          </form>
        </div>
    );
  }

  render () {
    if (this.state.changing) {
      return this.renderInput();
    }

    const hasValue = !!this.props.value;

    return (
        <div>
          {hasValue && (
              <i className="big-spacer-right icon-lock icon-gray"/>
          )}

          <button onClick={e => this.handleChangeClick(e)}>
            {hasValue ? translate('change_verb') : translate('set')}
          </button>
        </div>
    );
  }
}
