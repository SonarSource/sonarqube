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
import { sortNodes } from '../utils';
import ChevronDownIcon from './icons/ChevronDownIcon';
import ChevronUpIcon from './icons/ChevronUpIcon';

export default function CategoryLink({ node, location, headers, onToggle }) {
  const hasChild = node.pages && node.pages.length > 0;
  const prefix = process.env.GATSBY_USE_PREFIX === '1' ? '/' + process.env.GATSBY_DOCS_VERSION : '';
  const { slug } = node.fields;
  const isCurrentPage = location.pathname === prefix + slug;
  const open = location.pathname.startsWith(prefix + slug);
  return (
    <div>
      <h2 className={isCurrentPage || open ? 'active' : ''}>
        <Link to={slug} title={node.frontmatter.title}>
          {hasChild && open && <ChevronUpIcon />}
          {hasChild && !open && <ChevronDownIcon />}
          {node.frontmatter.title}
        </Link>
      </h2>
      {isCurrentPage && <HeadingsLink headers={headers} />}
      {hasChild &&
        open && (
          <div className="sub-menu">
            {sortNodes(node.pages).map(page => (
              <SubpageLink
                key={page.fields.slug}
                headers={headers}
                displayHeading={location.pathname === prefix + page.fields.slug}
                node={page}
              />
            ))}
          </div>
        )}
    </div>
  );
}
