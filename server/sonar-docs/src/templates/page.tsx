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
import { graphql } from 'gatsby';
import * as React from 'react';
import Helmet from 'react-helmet';
import { MarkdownHeading, MarkdownRemark, MarkdownRemarkConnection } from '../@types/graphql-types';
import HeaderList from '../components/HeaderList';

interface Props {
  data: {
    allMarkdownRemark: Pick<MarkdownRemarkConnection, 'edges'>;
    markdownRemark: Pick<MarkdownRemark, 'html' | 'headings' | 'frontmatter'>;
  };
  location: Location;
}

export default class Page extends React.PureComponent<Props> {
  baseUrl = '';

  componentDidMount() {
    if (window) {
      this.baseUrl = window.location.origin + '/';
    }
    const collapsables = document.getElementsByClassName('collapse');

    for (let i = 0; i < collapsables.length; i++) {
      collapsables[i].classList.add('close');
      const customBlockWrapper = collapsables[i].querySelector('.custom-block-body');
      if (customBlockWrapper) {
        let firstChild = customBlockWrapper.firstElementChild;
        if (firstChild) {
          firstChild.outerHTML = firstChild.outerHTML
            .replace(/<h2/gi, '<a href="#"')
            .replace(/<\/h2>/gi, '</a>');

          // We changed the element. It's reference is no longer correct in some
          // browsers. Fetch it again.
          firstChild = customBlockWrapper.firstElementChild;
          firstChild!.addEventListener('click', (event: Event & { currentTarget: HTMLElement }) => {
            event.preventDefault();
            if (
              event.currentTarget.parentElement &&
              event.currentTarget.parentElement.parentElement
            ) {
              event.currentTarget.parentElement.parentElement.classList.toggle('close');
            }
          });
        }
      }
    }
  }

  render() {
    const page = this.props.data.markdownRemark;
    const version = process.env.GATSBY_DOCS_VERSION || '';
    const mainTitle = 'SonarQube Docs';
    const pageTitle = page.frontmatter && page.frontmatter.title;

    let htmlPageContent = page.html || '';

    const realHeadingsList = removeExtraHeadings(htmlPageContent, page.headings || []);

    htmlPageContent = removeTableOfContents(htmlPageContent);
    htmlPageContent = createAnchorForHeadings(htmlPageContent, realHeadingsList);
    htmlPageContent = replaceDynamicLinks(htmlPageContent);
    htmlPageContent = replaceImageLinks(htmlPageContent);

    return (
      <>
        <Helmet title={pageTitle ? `${pageTitle} | ${mainTitle}` : mainTitle}>
          <html lang="en" />
          <link href={`/${version}/favicon.ico`} rel="icon" />
          <link
            href={this.baseUrl + this.props.location.pathname.replace(version, 'latest')}
            rel="canonical"
          />
          <script type="text/javascript">{`
            (function(window,document) {
              (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
              m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
              })(window, document,'script','https://www.google-analytics.com/analytics.js','ga');
              ga('create', 'UA-1880045-11' , 'auto');
              ga('send', 'pageview');
            })(window,document);
          `}</script>
        </Helmet>
        <HeaderList headers={realHeadingsList} />
        <h1>{pageTitle || mainTitle}</h1>
        <div
          className="markdown-content"
          // Safe: comes from the backend
          dangerouslySetInnerHTML={{ __html: htmlPageContent }}
        />
      </>
    );
  }
}

export const query = graphql`
  query($slug: String!) {
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

function replaceImageLinks(content: string) {
  const version = process.env.GATSBY_DOCS_VERSION || '';
  if (version !== '') {
    content = content.replace(/<img src="\/images\/(.*?)"/gim, `<img src="/${version}/images/$1"`);
  }
  return content;
}

function replaceDynamicLinks(content: string) {
  // Make outside link open in a new tab
  content = content.replace(
    /<a href="http(.*?)">(.*?)<\/a>/gim,
    '<a href="http$1" target="_blank">$2</a>'
  );

  // Render only the text part of links going inside the app
  return content.replace(
    /<a href="(.*)\/#(?:sonarqube|sonarcloud|sonarqube-admin)#.*?">(.*?)<\/a>/gim,
    '$2'
  );
}

/**
 * For the sidebar table of content, we do not want headers for sonarcloud,
 * collapsable container title, of table of contents headers.
 */
function removeExtraHeadings(content: string, headings: MarkdownHeading[]) {
  return headings
    .filter(
      heading =>
        content.indexOf(
          `<div class="custom-block collapse"><div class="custom-block-body"><h2>${
            heading.value
          }</h2>`
        ) < 0
    )
    .filter(heading => !heading.value || !heading.value.match(/Table of content/i))
    .filter(heading => {
      const regex = new RegExp(
        `<!-- sonarcloud -->[\\s\\S]*<h2>${heading.value!.replace(
          /[.*+?^${}()|[\]\\]/g,
          '\\$&'
        )}<\\/h2>[\\s\\S]*<!-- /sonarcloud -->`,
        'gim'
      );
      return !content.match(regex);
    });
}

function createAnchorForHeadings(content: string, headings: MarkdownHeading[]) {
  let counter = 1;
  headings.forEach(h => {
    if (h.depth === 2) {
      content = content.replace(
        `<h${h.depth}>${h.value}</h${h.depth}>`,
        `<h${h.depth} id="header-${counter}">${h.value}</h${h.depth}>`
      );
      counter++;
    }
  });
  return content;
}

function removeTableOfContents(content: string) {
  return content.replace(/<h[1-9]>Table Of Contents<\/h[1-9]>/i, '');
}
