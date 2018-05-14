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
import React from 'react';
import PropTypes from 'prop-types';
import { translate } from '../../../helpers/l10n';
import { SubmitButton } from '../../../components/ui/buttons';

export default class BulkUpdateForm extends React.PureComponent {
  static propTypes = {
    onSubmit: PropTypes.func.isRequired
  };

  handleSubmit = e => {
    e.preventDefault();

    const replace = this.refs.replace.value;
    const by = this.refs.by.value;

    this.props.onSubmit(replace, by);
  };

  render() {
    return (
      <form onSubmit={this.handleSubmit}>
        <div className="modal-field">
          <label htmlFor="bulk-update-replace">{translate('update_key.replace')}</label>
          <input
            id="bulk-update-replace"
            name="replace"
            placeholder={translate('update_key.replace_example')}
            ref="replace"
            required={true}
            type="text"
          />
        </div>

        <div className="modal-field">
          <label htmlFor="bulk-update-by">{translate('update_key.by')}</label>
          <input
            id="bulk-update-by"
            name="by"
            placeholder={translate('update_key.by_example')}
            ref="by"
            required={true}
            type="text"
          />
          <SubmitButton className="big-spacer-left" id="bulk-update-see-results">
            {translate('update_key.see_results')}
          </SubmitButton>
        </div>
      </form>
    );
  }
}
