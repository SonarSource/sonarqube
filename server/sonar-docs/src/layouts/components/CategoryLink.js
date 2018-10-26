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
    const { node, location, children, title, open } = this.props;
    const prefix = process.env.GATSBY_DOCS_VERSION ? '/' + process.env.GATSBY_DOCS_VERSION : '';
    const url = node ? node.frontmatter.url || node.fields.slug : '';
    const isCurrentPage = location.pathname === prefix + url;
    const linkTitle = node ? node.frontmatter.nav || node.frontmatter.title : '';
    return (
      <div>
        {node ? (
          <Link
            className={isCurrentPage || open ? 'page-indexes-link active' : 'page-indexes-link'}
            to={url}
            title={linkTitle}>
            {linkTitle}
          </Link>
        ) : (
          <a
            className={isCurrentPage || open ? 'page-indexes-link active' : 'page-indexes-link'}
            href="#"
            onClick={this.toggle}>
            {open ? <ChevronUpIcon /> : <ChevronDownIcon />}
            {title}
          </a>
        )}
        {children &&
          open && (
            <div className="sub-menu">
              {children.map(page => {
                const url = page.frontmatter.url || page.fields.slug;
                return (
                  <SubpageLink active={location.pathname === prefix + url} key={url} node={page} />
                );
              })}
            </div>
          )}
      </div>
    );
  }
}
