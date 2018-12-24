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
import * as React from 'react';
import * as classNames from 'classnames';
import { translate } from '../../helpers/l10n';

interface Props {
  canTurnToPrivate?: boolean;
  className?: string;
  onChange: (visibility: T.Visibility) => void;
  showDetails?: boolean;
  visibility?: T.Visibility;
}

export default class VisibilitySelector extends React.PureComponent<Props> {
  handlePublicClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onChange('public');
  };

  handlePrivateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onChange('private');
  };

  render() {
    return (
      <div className={classNames('visibility-select', this.props.className)}>
        <a
          className="link-base-color link-no-underline visibility-select-option"
          href="#"
          id="visibility-public"
          onClick={this.handlePublicClick}>
          <i
            className={classNames('icon-radio', {
              'is-checked': this.props.visibility === 'public'
            })}
          />
          <span className="spacer-left">{translate('visibility.public')}</span>
        </a>
        {this.props.showDetails && (
          <span className="visibility-details note">
            {translate('visibility.public.description.long')}
          </span>
        )}

        {this.props.canTurnToPrivate ? (
          <>
            <a
              className="link-base-color link-no-underline huge-spacer-left visibility-select-option"
              href="#"
              id="visibility-private"
              onClick={this.handlePrivateClick}>
              <i
                className={classNames('icon-radio', {
                  'is-checked': this.props.visibility === 'private'
                })}
              />
              <span className="spacer-left">{translate('visibility.private')}</span>
            </a>
            {this.props.showDetails && (
              <span className="visibility-details note">
                {translate('visibility.private.description.long')}
              </span>
            )}
          </>
        ) : (
          <>
            <span
              className="huge-spacer-left text-muted cursor-not-allowed visibility-select-option"
              id="visibility-private">
              <i
                className={classNames('icon-radio', {
                  'is-checked': this.props.visibility === 'private'
                })}
              />
              <span className="spacer-left">{translate('visibility.private')}</span>
            </span>
            {this.props.showDetails && (
              <span className="visibility-details note">
                {translate('visibility.private.description.long')}
              </span>
            )}
          </>
        )}
      </div>
    );
  }
}
