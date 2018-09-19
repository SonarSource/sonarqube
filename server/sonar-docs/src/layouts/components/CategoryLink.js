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
import * as React from 'react';
import Link from 'gatsby-link';
import SubpageLink from './SubpageLink';
import HeadingsLink from './HeadingsLink';
import ChevronDownIcon from './icons/ChevronDownIcon';
import ChevronUpIcon from './icons/ChevronUpIcon';

export default class CategoryLink extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = { open: props.open };
  }

  toggle = event => {
    event.preventDefault();
    event.stopPropagation();
    this.props.onToggle(this.props.title);
  };

  render() {
    const { node, location, headers, children, title, open } = this.props;
    const prefix =
      process.env.GATSBY_USE_PREFIX === '1' ? '/' + process.env.GATSBY_DOCS_VERSION : '';
    const url = node ? node.frontmatter.url || node.fields.slug : '';
    const isCurrentPage = location.pathname === prefix + url;
    return (
      <div>
        <h2 className={isCurrentPage || open ? 'active' : ''}>
          {node ? (
            <Link to={url} title={node.frontmatter.title}>
              {node.frontmatter.title}
            </Link>
          ) : (
            <a href="#" onClick={this.toggle}>
              {open ? <ChevronUpIcon /> : <ChevronDownIcon />}
              {title}
            </a>
          )}
        </h2>
        {isCurrentPage && <HeadingsLink headers={headers} />}
        {children &&
          open && (
            <div className="sub-menu">
              {children.map(page => {
                const url = page.frontmatter.url || page.fields.slug;
                return (
                  <SubpageLink
                    displayHeading={location.pathname === prefix + url}
                    headers={headers}
                    key={url}
                    node={page}
                  />
                );
              })}
            </div>
          )}
      </div>
    );
  }
}
