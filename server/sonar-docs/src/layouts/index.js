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
import 'babel-polyfill';
import React from 'react';
import Sidebar from './components/Sidebar';
import Footer from './components/Footer';
import HeaderListProvider from './components/HeaderListProvider';
import HeadingsLink from './components/HeadingsLink';

const version = process.env.GATSBY_DOCS_VERSION || '1.0';

export default function Layout(props) {
  return (
    <div className="main-container">
      <div className="blue-bar" />
      <HeaderListProvider>
        {({ headers }) => (
          <div className="layout-page">
            <div className="page-sidebar-inner">
              <Sidebar
                location={props.location}
                pages={props.data.allMarkdownRemark.edges
                  .map(e => e.node)
                  .filter(n => !n.fields.slug.startsWith('/tooltips'))}
                searchIndex={props.data.siteSearchIndex}
                version={version}
              />
            </div>
            <div className="page-main">
              <div className="page-container">
                <HeadingsLink headers={headers} />
                <div className="markdown-container">{props.children()}</div>
              </div>
              <Footer />
            </div>
          </div>
        )}
      </HeaderListProvider>
    </div>
  );
}

export const query = graphql`
  query IndexQuery {
    allMarkdownRemark {
      edges {
        node {
          id
          headings {
            depth
            value
          }
          frontmatter {
            title
            nav
            url
          }
          fields {
            slug
          }
          html
        }
      }
    }
  }
`;
