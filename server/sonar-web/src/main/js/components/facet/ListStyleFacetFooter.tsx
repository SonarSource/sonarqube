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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';

interface Props {
  className?: string;
  count: number;
  showMore: () => void;
  showLess: (() => void) | undefined;
  total: number;
}

export default class ListStyleFacetFooter extends React.PureComponent<Props> {
  handleShowMoreClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.showMore();
  };

  handleShowLessClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.props.showLess) {
      this.props.showLess();
    }
  };

  render() {
    const { count, total } = this.props;
    const hasMore = total > count;
    const allShown = Boolean(total && total === count);

    return (
      <footer className="note spacer-top spacer-bottom text-center">
        {translateWithParameters('x_show', formatMeasure(count, 'INT', null))}

        {hasMore && (
          <a className="spacer-left text-muted" href="#" onClick={this.handleShowMoreClick}>
            {translate('show_more')}
          </a>
        )}

        {this.props.showLess && allShown && (
          <a className="spacer-left text-muted" href="#" onClick={this.handleShowLessClick}>
            {translate('show_less')}
          </a>
        )}
      </footer>
    );
  }
}
