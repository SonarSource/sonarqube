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
import classNames from 'classnames';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';

/*::
type Props = {|
  allowMore: boolean,
  loadingMore: ?string,
  onMoreClick: string => void,
  onSelect: string => void,
  qualifier: string,
  selected: boolean
|};
*/

export default class SearchShowMore extends React.PureComponent {
  /*:: props: Props; */

  handleMoreClick = (event /*: MouseEvent & { currentTarget: HTMLElement } */) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    const { qualifier } = event.currentTarget.dataset;
    this.props.onMoreClick(qualifier);
  };

  handleMoreMouseEnter = (event /*: { currentTarget: HTMLElement } */) => {
    const { qualifier } = event.currentTarget.dataset;
    this.props.onSelect(`qualifier###${qualifier}`);
  };

  render() {
    const { loadingMore, qualifier, selected } = this.props;

    return (
      <li key={`more-${qualifier}`} className={classNames('menu-footer', { active: selected })}>
        <DeferredSpinner className="navbar-search-icon" loading={loadingMore === qualifier}>
          <a
            className={classNames({ 'cursor-not-allowed': !this.props.allowMore })}
            data-qualifier={qualifier}
            href="#"
            onClick={this.handleMoreClick}
            onMouseEnter={this.handleMoreMouseEnter}>
            <div
              className="pull-right text-muted-2 menu-footer-note"
              dangerouslySetInnerHTML={{
                __html: translateWithParameters(
                  'search.show_more.hint',
                  '<span class="shortcut-button shortcut-button-small">Enter</span>'
                )
              }}
            />
            <span>{translate('show_more')}</span>
          </a>
        </DeferredSpinner>
      </li>
    );
  }
}
