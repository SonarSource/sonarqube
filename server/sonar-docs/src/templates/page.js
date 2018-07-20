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
import './page.css';

export default class Page extends React.PureComponent {
  componentDidMount() {
    const collaspables = document.getElementsByClassName('collapse');
    for (let i = 0; i < collaspables.length; i++) {
      collaspables[i].classList.add('close');
      collaspables[i].firstChild.outerHTML = collaspables[i].firstChild.outerHTML
        .replace(/\<h2/gi, '<a href="#"')
        .replace(/\<\/h2\>/gi, '</a>');
      collaspables[i].firstChild.addEventListener('click', e => {
        e.currentTarget.parentNode.classList.toggle('close');
        e.preventDefault();
      });
    }
  }

  render() {
    const page = this.props.data.markdownRemark;
    let htmlWithInclusions = cutSonarCloudContent(page.html).replace(
      /\<p\>@include (.*)\<\/p\>/,
      (_, path) => {
        const chunk = data.allMarkdownRemark.edges.find(edge => edge.node.fields.slug === path);
        return chunk ? chunk.node.html : '';
      }
    );

    if (
      page.headings &&
      page.headings.length > 0 &&
      page.html.match(/<h[1-9]>Table Of Contents<\/h[1-9]>/i)
    ) {
      htmlWithInclusions = generateTableOfContents(htmlWithInclusions, page.headings);
    }

    htmlWithInclusions = replaceDynamicLinks(htmlWithInclusions);

    return (
      <div css={{ paddingTop: 24, paddingBottom: 24 }}>
        <Helmet title={page.frontmatter.title} />
        <h1>{page.frontmatter.title}</h1>
        <div
          css={{
            '& img[src$=".svg"]': {
              position: 'relative',
              top: '-2px',
              verticalAlign: 'text-bottom'
            }
          }}
          dangerouslySetInnerHTML={{ __html: htmlWithInclusions }}
        />
      </div>
    );
  }
}

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
      headings {
        depth
        value
      }
      frontmatter {
        title
      }
    }
  }
`;

function replaceDynamicLinks(content) {
  return content.replace(
    /\<a href="\/#(?:sonarqube|sonarcloud|sonarqube-admin)#.*"\>(.*)\<\/a\>/gim,
    '$1'
  );
}

function generateTableOfContents(content, headings) {
  let html = '<h2>Table Of Contents</h2>';
  let depth = headings[0].depth - 1;
  for (let i = 1; i < headings.length; i++) {
    // Do not include title from collapsible content
    if (
      content.match(new RegExp(`\<div class="collapse"\>\<h2\>${headings[i].value}\<\/h2\>`, 'gi'))
    ) {
      continue;
    }

    while (headings[i].depth > depth) {
      html += '<ul>';
      depth++;
    }
    while (headings[i].depth < depth) {
      html += '</ul>';
      depth--;
    }
    html += `<li><a href="#header-${i}">${headings[i].value}</a></li>`;
    content = content.replace(
      new RegExp(`<h${headings[i].depth}>${headings[i].value}</h${headings[i].depth}>`, 'gi'),
      `<h${headings[i].depth} id="header-${i}">${headings[i].value}</h${headings[i].depth}>`
    );
  }
  html += '</ul>';
  return content.replace(/<h[1-9]>Table Of Contents<\/h[1-9]>/i, html);
}

function cutSonarCloudContent(content) {
  const beginning = '<!-- sonarcloud -->';
  const ending = '<!-- /sonarcloud -->';

  let newContent = content;
  let start = newContent.indexOf(beginning);
  let end = newContent.indexOf(ending);
  while (start !== -1 && end !== -1) {
    newContent = newContent.substring(0, start) + newContent.substring(end + ending.length);
    start = newContent.indexOf(beginning);
    end = newContent.indexOf(ending);
  }

  return newContent;
}
