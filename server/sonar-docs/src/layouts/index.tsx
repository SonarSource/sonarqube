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
import { graphql, StaticQuery } from 'gatsby';
import * as React from 'react';
import { MarkdownRemark, MarkdownRemarkConnection } from '../@types/graphql-types';
import Footer from '../components/Footer';
import HeaderListProvider from '../components/HeaderListProvider';
import HeadingsLink from '../components/HeadingsLink';
import PluginMetaData from '../components/PluginMetaData';
import Sidebar from '../components/Sidebar';
import './layout.css';

const version = process.env.GATSBY_DOCS_VERSION || '1.0';

interface Props {
  children: React.ReactNode;
  location: Location;
}

export default function Layout({ children, location }: Props) {
  return (
    <div className="main-container">
      <div className="blue-bar" />
      <HeaderListProvider>
        {({ headers }) => (
          <div className="layout-page">
            <div className="page-sidebar-inner">
              <StaticQuery
                query={graphql`
                  {
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
                `}
                render={(data: { allMarkdownRemark: MarkdownRemarkConnection }) =>
                  data.allMarkdownRemark &&
                  data.allMarkdownRemark.edges && (
                    <Sidebar
                      location={location}
                      pages={
                        data.allMarkdownRemark.edges
                          .map(e => e.node)
                          .filter(
                            n =>
                              n &&
                              n.fields &&
                              n.fields.slug &&
                              !n.fields.slug.startsWith('/tooltips')
                          ) as MarkdownRemark[]
                      }
                      version={version}
                    />
                  )
                }
              />
            </div>
            <div className="page-main">
              <div className="page-container">
                <HeadingsLink headers={headers} />
                <div className="markdown-container">{children}</div>
              </div>
              <Footer />
              <PluginMetaData location={location} />
            </div>
          </div>
        )}
      </HeaderListProvider>
    </div>
  );
}
