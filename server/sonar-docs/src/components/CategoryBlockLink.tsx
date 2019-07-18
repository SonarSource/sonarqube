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
import classNames from 'classnames';
import * as React from 'react';
import { MarkdownRemark } from '../@types/graphql-types';
import ChevronDownIcon from './icons/ChevronDownIcon';
import ChevronUpIcon from './icons/ChevronUpIcon';
import PageLink from './PageLink';

interface Props {
  children: (MarkdownRemark | JSX.Element)[];
  location: Location;
  openByDefault: boolean;
  title: string;
}

interface State {
  open: boolean;
}

export default class CategoryLink extends React.PureComponent<Props, State> {
  state: State;

  static defaultProps = {
    openByDefault: false
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      open: props.openByDefault
    };
  }

  handleToggle = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.setState(prevState => ({
      open: !prevState.open
    }));
  };

  isMarkdownRemark = (child: any): child is MarkdownRemark => {
    return child.id !== undefined;
  };

  render() {
    const { children, location, title } = this.props;
    const { open } = this.state;
    return (
      <div>
        <a
          className={classNames('page-indexes-link', { active: open })}
          href="#"
          onClick={this.handleToggle}>
          {open ? <ChevronUpIcon /> : <ChevronDownIcon />}
          {title}
        </a>
        {children && open && (
          <div className="sub-menu">
            {children.map((child, i) => {
              if (this.isMarkdownRemark(child)) {
                return (
                  <PageLink
                    className="sub-menu-link"
                    key={child.id}
                    location={location}
                    node={child}
                  />
                );
              } else {
                return <React.Fragment key={`child-${i}`}>{child}</React.Fragment>;
              }
            })}
          </div>
        )}
      </div>
    );
  }
}
