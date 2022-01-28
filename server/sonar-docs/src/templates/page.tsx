/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { selectAll } from 'hast-util-select';
import * as React from 'react';
import Helmet from 'react-helmet';
import rehypeReact from 'rehype-react';
import { MarkdownHeading, MarkdownRemark, MarkdownRemarkConnection } from '../@types/graphql-types';
import HeaderList from '../components/HeaderList';
import MetaData from '../components/MetaData';
import { HtmlAST, HtmlASTNode } from '../types/hast';

interface Props {
  data: {
    allMarkdownRemark: Pick<MarkdownRemarkConnection, 'edges'>;
    markdownRemark: Pick<MarkdownRemark, 'htmlAst' | 'headings' | 'frontmatter'>;
  };
  location: Location;
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
      htmlAst
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

export default class Page extends React.PureComponent<Props> {
  baseUrl = '';

  // @ts-ignore
  renderAst = new rehypeReact({
    createElement: React.createElement,
    components: {
      'update-center': ({ updatecenterkey }: { updatecenterkey: string }) => (
        <MetaData updateCenterKey={updatecenterkey} />
      )
    }
  }).Compiler;

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

    page.headings = filterHeaderList(page.htmlAst, page.headings);
    addSlugToHeader(page.htmlAst);
    makeExternalLinkOpenInNewTab(page.htmlAst);
    removeInAppLinks(page.htmlAst);
    addDocVersionToImagesLinks(page.htmlAst);

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
        <HeaderList headers={page.headings || []} />
        <h1>{pageTitle || mainTitle}</h1>
        <div className="markdown-content">{this.renderAst(page.htmlAst)}</div>
      </>
    );
  }
}

function filterHeaderList(hast: HtmlAST, headers: MarkdownHeading[] | null) {
  if (!headers) {
    return null;
  }

  // Keep only first level h2
  return headers.filter(header =>
    hast.children.some(
      elt =>
        elt.tagName === 'h2' &&
        elt.children &&
        elt.children.some(child => child.value === header.value)
    )
  );
}

function addSlugToHeader(hast: HtmlAST) {
  let counter = 1;

  hast.children.forEach(elt => {
    if (elt.tagName === 'h2') {
      elt.properties = { ...elt.properties, id: `header-${counter}` };
      counter++;
    }
  });
}

function makeExternalLinkOpenInNewTab(hast: HtmlAST) {
  selectAll('a[href^=http]', hast).forEach(
    (elt: HtmlASTNode) => (elt.properties = { ...elt.properties, target: '_blank' })
  );
}

function removeInAppLinks(hast: HtmlAST) {
  const inAppLinksTags = ['/#sonarqube#/', '/#sonarcloud#/', '/#sonarqube-admin#/'];

  selectAll(inAppLinksTags.map(tag => `a[href*=${tag}]`).join(','), hast).forEach(
    (elt: HtmlASTNode) => {
      elt.tagName = 'span';
      delete elt.properties?.href;
    }
  );
}

function addDocVersionToImagesLinks(hast: HtmlAST) {
  const version = process.env.GATSBY_DOCS_VERSION || '';
  const imgPrefix = 'images';

  if (version !== '') {
    selectAll(`img[src^=/${imgPrefix}/]`, hast).forEach((elt: HtmlASTNode) => {
      if (elt.properties?.src) {
        elt.properties.src = elt.properties.src.replace(imgPrefix, `${version}/${imgPrefix}`);
      }
    });
  }
}
