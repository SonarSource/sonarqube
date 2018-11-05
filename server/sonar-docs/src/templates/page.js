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
import HeaderList from '../layouts/components/HeaderList';
import './page.css';

const version = process.env.GATSBY_DOCS_VERSION || '1.0';

export default class Page extends React.PureComponent {
  baseUrl = '';

  componentDidMount() {
    if (window) {
      this.baseUrl = window.location.origin + '/';
    }
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

    const realHeadingsList = removeExtraHeadings(page.html, page.headings);
    htmlWithInclusions = removeTableOfContents(htmlWithInclusions);
    htmlWithInclusions = createAnchorForHeadings(htmlWithInclusions, realHeadingsList);
    htmlWithInclusions = replaceDynamicLinks(htmlWithInclusions);
    htmlWithInclusions = replaceImageLinks(htmlWithInclusions);
    htmlWithInclusions = replaceInstanceTag(htmlWithInclusions);

    const version = process.env.GATSBY_DOCS_VERSION || '';

    return (
      <div css={{ paddingTop: 24, paddingBottom: 24 }}>
        <Helmet title={page.frontmatter.title || 'Documentation'}>
          <html lang="en" />
          <link rel="icon" href={`/${version}/favicon.ico`} />
          <link
            rel="canonical"
            href={this.baseUrl + this.props.location.pathname.replace(version, 'latest')}
          />
        </Helmet>
        <HeaderList headers={realHeadingsList} />
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

function replaceInstanceTag(content) {
  return content.replace(/{instance}/gi, 'SonarQube');
}

function replaceImageLinks(content) {
  const version = process.env.GATSBY_DOCS_VERSION || '';
  if (version !== '') {
    content = content.replace(/\<img src="\/images\/(.*)"/gim, `<img src="/${version}/images/$1"`);
  }
  return content;
}

function replaceDynamicLinks(content) {
  // Make outside link open in a new tab
  content = content.replace(
    /\<a href="http(.*)"\>(.*)\<\/a\>/gim,
    '<a href="http$1" target="_blank">$2</a>'
  );

  return content.replace(
    /\<a href="(.*)\/#(?:sonarqube|sonarcloud|sonarqube-admin)#.*"\>(.*)\<\/a\>/gim,
    '$2'
  );
}

/**
 * For the sidebar table of content, we do not want headers for sonarcloud,
 * collapsable container title, of table of contents headers.
 */
function removeExtraHeadings(content, headings) {
  return headings
    .filter(heading => content.indexOf(`<div class="collapse"><h2>${heading.value}</h2>`) < 0)
    .filter(heading => !heading.value.match(/Table of content/i))
    .filter(heading => {
      const regex = new RegExp(
        '<!-- sonarcloud -->[\\s\\S]*<h2>' + heading.value + '<\\/h2>[\\s\\S]*<!-- /sonarcloud -->',
        'gim'
      );
      return !content.match(regex);
    });
}

function removeSonarCloudHeadings(content, headings) {
  return headings.filter(
    heading => content.indexOf(`<div class="collapse"><h2>${heading.value}</h2>`) < 0
  );
}

function createAnchorForHeadings(content, headings) {
  let counter = 1;
  headings.map(h => {
    if (h.depth == 2) {
      content = content.replace(
        `<h${h.depth}>${h.value}</h${h.depth}>`,
        `<h${h.depth} id="header-${counter}">${h.value}</h${h.depth}>`
      );
      counter++;
    }
  });
  return content;
}

function removeTableOfContents(content) {
  return content.replace(/<h[1-9]>Table Of Contents<\/h[1-9]>/i, '');
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
