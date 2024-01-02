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
import * as React from 'react';
import Radio from '../../components/controls/Radio';
import { translate } from '../../helpers/l10n';
import { Visibility } from '../../types/types';

interface Props {
  canTurnToPrivate?: boolean;
  className?: string;
  onChange: (visibility: Visibility) => void;
  showDetails?: boolean;
  visibility?: Visibility;
}

export default class VisibilitySelector extends React.PureComponent<Props> {
  render() {
    return (
      <div className={classNames(this.props.className)}>
        {['public', 'private'].map((visibility) => (
          <Radio
            className={`huge-spacer-right visibility-${visibility}`}
            key={visibility}
            value={visibility}
            checked={this.props.visibility === visibility}
            onCheck={this.props.onChange}
            disabled={visibility === 'private' && !this.props.canTurnToPrivate}
          >
            <div>
              {translate('visibility', visibility)}
              {this.props.showDetails && (
                <p className="note">{translate('visibility', visibility, 'description.long')}</p>
              )}
            </div>
          </Radio>
        ))}
      </div>
    );
  }
}
