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
import * as navigationTreeSonarCloud from 'Docs/../static/SonarCloudNavigationTree.json';
import * as navigationTreeSonarQube from 'Docs/../static/SonarQubeNavigationTree.json';
import { DocNavigationItem } from 'Docs/@types/types';
import * as React from 'react';
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addSideBarClass, removeSideBarClass } from 'sonar-ui-common/helpers/pages';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getInstalledPlugins } from '../../../api/plugins';
import { getPluginStaticFileContent } from '../../../api/static';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import NotFound from '../../../app/components/NotFound';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import DocMarkdownBlock from '../../../components/docs/DocMarkdownBlock';
import { ParsedContent, separateFrontMatter } from '../../../helpers/markdown';
import { isSonarCloud } from '../../../helpers/system';
import { getUrlsList } from '../navTreeUtils';
import getPages from '../pages';
import '../styles.css';
import { DocumentationEntry } from '../utils';
import Sidebar from './Sidebar';

interface Props {
  params: { splat?: string };
}

interface State {
  loading: boolean;
  pages: DocumentationEntry[];
  tree: DocNavigationItem[];
}

const LANGUAGES_BASE_URL = 'analysis/languages';

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: false,
    pages: [],
    tree: []
  };

  componentDidMount() {
    this.mounted = true;
    addSideBarClass();

    this.setState({ loading: true });

    const tree = isSonarCloud()
      ? ((navigationTreeSonarCloud as any).default as DocNavigationItem[])
      : ((navigationTreeSonarQube as any).default as DocNavigationItem[]);

    this.getLanguagePluginsDocumentation(tree).then(
      overrides => {
        if (this.mounted) {
          this.setState({
            loading: false,
            pages: getPages(overrides),
            tree
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({
            loading: false
          });
        }
      }
    );
  }

  componentWillUnmount() {
    this.mounted = false;
    removeSideBarClass();
  }

  getLanguagePluginsDocumentation = (tree: DocNavigationItem[]) => {
    return getInstalledPlugins()
      .then(plugins =>
        Promise.all(
          plugins.map(plugin => {
            if (plugin.documentationPath) {
              const matchArray = /^static\/(.*)/.exec(plugin.documentationPath);

              if (matchArray && matchArray.length > 1) {
                // eslint-disable-next-line promise/no-nesting
                return getPluginStaticFileContent(plugin.key, matchArray[1]).then(
                  content => content,
                  () => undefined
                );
              }
            }
            return undefined;
          })
        )
      )
      .then(contents => contents.filter(isDefined))
      .then(contents => {
        const regex = new RegExp(`/${LANGUAGES_BASE_URL}/\\w+/$`);
        const overridablePaths = getUrlsList(tree).filter(
          path => regex.test(path) && path !== `/${LANGUAGES_BASE_URL}/overview/`
        );

        const parsedContent: T.Dict<ParsedContent> = {};

        contents.forEach(content => {
          const parsed = separateFrontMatter(content);
          if (
            parsed &&
            parsed.frontmatter &&
            parsed.frontmatter.key &&
            overridablePaths.includes(`/${LANGUAGES_BASE_URL}/${parsed.frontmatter.key}/`)
          ) {
            parsedContent[`${LANGUAGES_BASE_URL}/${parsed.frontmatter.key}`] = parsed;
          }
        });

        return parsedContent;
      });
  };

  render() {
    const { loading, pages, tree } = this.state;
    const { splat = '' } = this.props.params;

    if (loading) {
      return (
        <div className="page page-limited">
          <DeferredSpinner />
        </div>
      );
    }

    const page = pages.find(p => p.url === '/' + splat);
    const mainTitle = translate(
      'documentation.page_title',
      isSonarCloud() ? 'sonarcloud' : 'sonarqube'
    );
    const isIndex = splat === 'index';

    if (!page) {
      return (
        <>
          <Helmet title={mainTitle}>
            <meta content="noindex nofollow" name="robots" />
          </Helmet>
          <A11ySkipTarget anchor="documentation_main" />
          <NotFound withContainer={false} />
        </>
      );
    }

    return (
      <div className="layout-page">
        <Helmet title={isIndex || !page.title ? mainTitle : `${page.title} | ${mainTitle}`}>
          {!isSonarCloud() && <meta content="noindex nofollow" name="robots" />}
        </Helmet>

        <ScreenPositionHelper className="layout-page-side-outer">
          {({ top }) => (
            <div className="layout-page-side" style={{ top }}>
              <div className="layout-page-side-inner">
                <div className="layout-page-filters">
                  <div className="documentation-page-header">
                    <A11ySkipTarget
                      anchor="documentation_menu"
                      label={translate('documentation.skip_to_nav')}
                      weight={10}
                    />

                    <Link to="/documentation/">
                      <h1>{translate('documentation.page')}</h1>
                    </Link>
                  </div>
                  <Sidebar navigation={tree} pages={pages} splat={splat} />
                </div>
              </div>
            </div>
          )}
        </ScreenPositionHelper>

        <div className="layout-page-main">
          <div className="layout-page-main-inner">
            <div className="boxed-group">
              <A11ySkipTarget anchor="documentation_main" />

              <DocMarkdownBlock
                className="documentation-content cut-margins boxed-group-inner"
                content={page.content}
                stickyToc={true}
                title={page.title}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
}
