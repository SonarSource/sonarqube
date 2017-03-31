/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { isProvided, isClickable } from '../../project-admin/links/utils';
import BugTrackerIcon from '../../../components/ui/BugTrackerIcon';

type Link = {
  id: string,
  name: string,
  url: string,
  type: string
};

type State = {|
  expanded: boolean
|};

export default class MetaLink extends React.Component {
  props: {
    link: Link
  };

  state: State = {
    expanded: false
  };

  handleClick = (e: Object) => {
    e.preventDefault();
    e.target.blur();
    this.setState((s: State): State => ({ expanded: !s.expanded }));
  };

  renderLinkIcon(link: Link) {
    if (link.type === 'issue') {
      return <BugTrackerIcon />;
    }

    return isProvided(link)
      ? <i className={`icon-color-link icon-${link.type}`} />
      : <i className="icon-color-link icon-detach" />;
  }

  render() {
    const { link } = this.props;

    return (
      <li>
        <a
          className="link-with-icon"
          href={link.url}
          target="_blank"
          onClick={!isClickable(link) && this.handleClick}>
          {this.renderLinkIcon(link)}
          &nbsp;
          {link.name}
        </a>
        {this.state.expanded &&
          <div className="little-spacer-top">
            <input
              type="text"
              className="overview-key"
              value={link.url}
              readOnly={true}
              onClick={e => e.target.select()}
            />
          </div>}
      </li>
    );
  }
}
