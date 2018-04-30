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
import React from 'react';
import Helmet from 'react-helmet';

export default ({ data }) => {
  const page = data.markdownRemark;
  const htmlWithInclusions = page.html.replace(/\<p\>@include (.*)\<\/p\>/, (_, path) => {
    const chunk = data.allMarkdownRemark.edges.find(edge => edge.node.fields.slug === path);
    return chunk ? chunk.node.html : '';
  });

  return (
    <div css={{ paddingTop: 24, paddingBottom: 24 }}>
      <Helmet title={page.frontmatter.title} />
      <h1>{page.frontmatter.title}</h1>
      <div dangerouslySetInnerHTML={{ __html: htmlWithInclusions }} />
    </div>
  );
};

export const query = graphql`
  query PageQuery($slug: String!) {
    allMarkdownRemark {
      edges {
        node {
          html
          fields {
            slug
          }
        }
      }
    }
    markdownRemark(fields: { slug: { eq: $slug } }) {
      html
      frontmatter {
        title
      }
    }
  }
`;
