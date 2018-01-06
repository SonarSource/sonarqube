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
import { getSettingValue, isEmptyValue, getDefaultValue } from '../utils';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  isDefault: boolean,
  onReset: () => void,
  setting: Object
};
*/
/*::
type State = { reseting: boolean };
*/

export default class DefinitionDefaults extends React.PureComponent {
  /*:: props: Props; */
  state /*: State*/ = { reseting: false };

  handleClose = () => {
    this.setState({ reseting: false });
  };

  handleReset = (e /*: Event & {target: HTMLElement} */) => {
    e.preventDefault();
    e.target.blur();
    this.setState({ reseting: true });
  };

  handleSubmit = (event /*: Event */) => {
    event.preventDefault();
    this.props.onReset();
    this.handleClose();
  };

  renderModal() {
    const header = translate('settings.reset_confirm.title');
    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <p>{translate('settings.reset_confirm.description')}</p>
          </div>
          <footer className="modal-foot">
            <button className="button-red">{translate('reset_verb')}</button>
            <button type="reset" className="button-link" onClick={this.handleClose}>
              {translate('cancel')}
            </button>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    const { setting, isDefault } = this.props;
    const { definition } = setting;

    const isExplicitlySet = !isDefault && !isEmptyValue(definition, getSettingValue(setting));

    return (
      <div>
        {isDefault && (
          <div className="spacer-top note" style={{ lineHeight: '24px' }}>
            {translate('settings._default')}
          </div>
        )}

        {isExplicitlySet && (
          <div className="spacer-top nowrap">
            <button onClick={this.handleReset}>{translate('reset_verb')}</button>
            <span className="spacer-left note">
              {translate('default')}
              {': '}
              {getDefaultValue(setting)}
            </span>
          </div>
        )}
        {this.state.reseting && this.renderModal()}
      </div>
    );
  }
}
