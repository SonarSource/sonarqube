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
import { some } from 'lodash';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { Button } from '../../../components/ui/buttons';

export default class BulkUpdateResults extends React.PureComponent {
  static propTypes = {
    results: PropTypes.array.isRequired,
    onConfirm: PropTypes.func.isRequired
  };

  render() {
    const { results, replace, by } = this.props;
    const isEmpty = results.length === 0;
    const hasDuplications = some(results, r => r.duplicate);
    const canUpdate = !isEmpty && !hasDuplications;

    return (
      <div className="big-spacer-top" id="bulk-update-simulation">
        {isEmpty && (
          <div className="spacer-bottom" id="bulk-update-nothing">
            {translateWithParameters('update_key.no_key_to_update', replace)}
          </div>
        )}

        {hasDuplications && (
          <div className="spacer-bottom" id="bulk-update-duplicate">
            {translateWithParameters('update_key.cant_update_because_duplicate_keys', replace, by)}
          </div>
        )}

        {canUpdate && (
          <div className="spacer-bottom">
            {translate('update_key.keys_will_be_updated_as_follows')}
          </div>
        )}

        {!isEmpty && (
          <table className="data zebra zebra-hover" id="bulk-update-results">
            <thead>
              <tr>
                <th>{translate('update_key.old_key')}</th>
                <th>{translate('update_key.new_key')}</th>
              </tr>
            </thead>
            <tbody>
              {results.map(result => (
                <tr data-key={result.key} key={result.key}>
                  <td className="js-old-key">{result.key}</td>
                  <td className="js-new-key">
                    {result.duplicate && (
                      <span className="spacer-right badge badge-danger">
                        {translate('update_key.duplicate_key')}
                      </span>
                    )}
                    {result.newKey}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="big-spacer-top">
          {canUpdate && (
            <Button id="bulk-update-confirm" onClick={this.props.onConfirm}>
              {translate('update_verb')}
            </Button>
          )}
        </div>
      </div>
    );
  }
}
