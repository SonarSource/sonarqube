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
import { getLinkName } from '../../projectLinks/utils';
import ProjectLinkIcon from '../../../components/icons-components/ProjectLinkIcon';
import isValidUri from '../../../app/utils/isValidUri';
import ClearIcon from '../../../components/icons-components/ClearIcon';
import './MetaLink.css';

interface Props {
  iconOnly?: boolean;
  link: T.ProjectLink;
}

interface State {
  expanded: boolean;
}

export default class MetaLink extends React.PureComponent<Props, State> {
  state = {
    expanded: false
  };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState(s => ({ expanded: !s.expanded }));
  };

  render() {
    const { iconOnly, link } = this.props;
    const linkTitle = getLinkName(link);
    return (
      <li>
        <a
          className="link-with-icon"
          href={link.url}
          onClick={!isValidUri(link.url) ? this.handleClick : undefined}
          rel="nofollow noreferrer noopener"
          target="_blank"
          title={linkTitle}>
          <ProjectLinkIcon className="little-spacer-right" type={link.type} />
          {!iconOnly && linkTitle}
        </a>
        {this.state.expanded && (
          <div className="little-spacer-top copy-paste-link">
            <input
              className="overview-key"
              onClick={(event: React.MouseEvent<HTMLInputElement>) => event.currentTarget.select()}
              readOnly={true}
              type="text"
              value={link.url}
            />
            <a className="close" href="#" onClick={this.handleClick}>
              <ClearIcon />
            </a>
          </div>
        )}
      </li>
    );
  }
}
