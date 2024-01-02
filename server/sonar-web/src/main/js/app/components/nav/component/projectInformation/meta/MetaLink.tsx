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
import * as React from 'react';
import { ClearButton } from '../../../../../../components/controls/buttons';
import ProjectLinkIcon from '../../../../../../components/icons/ProjectLinkIcon';
import { getLinkName } from '../../../../../../helpers/projectLinks';
import { ProjectLink } from '../../../../../../types/types';
import isValidUri from '../../../../../utils/isValidUri';

interface Props {
  iconOnly?: boolean;
  link: ProjectLink;
}

interface State {
  expanded: boolean;
}

export default class MetaLink extends React.PureComponent<Props, State> {
  state = { expanded: false };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState(({ expanded }) => ({ expanded: !expanded }));
  };

  handleCollapse = () => {
    this.setState({ expanded: false });
  };

  handleSelect = (event: React.MouseEvent<HTMLInputElement>) => {
    event.currentTarget.select();
  };

  render() {
    const { iconOnly, link } = this.props;
    const linkTitle = getLinkName(link);
    const isValid = isValidUri(link.url);
    return (
      <li>
        <a
          className="link-no-underline"
          href={isValid ? link.url : undefined}
          onClick={isValid ? undefined : this.handleClick}
          rel="nofollow noreferrer noopener"
          target="_blank"
          title={linkTitle}
        >
          <ProjectLinkIcon className="little-spacer-right" type={link.type} />
          {!iconOnly && linkTitle}
        </a>
        {this.state.expanded && (
          <div className="little-spacer-top display-flex-center">
            <input
              className="overview-key width-80"
              onClick={this.handleSelect}
              readOnly={true}
              type="text"
              value={link.url}
            />
            <ClearButton className="little-spacer-left" onClick={this.handleCollapse} />
          </div>
        )}
      </li>
    );
  }
}
