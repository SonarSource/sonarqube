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
import * as navigationTreeSonarQube from 'Docs/../static/SonarQubeNavigationTree.json';
import { DocNavigationItem } from 'Docs/@types/types';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router';
import { getInstalledPlugins } from '../../../api/plugins';
import { getPluginStaticFileContent } from '../../../api/static';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import NotFound from '../../../app/components/NotFound';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import DocMarkdownBlock from '../../../components/docs/DocMarkdownBlock';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { ParsedContent, separateFrontMatter } from '../../../helpers/markdown';
import { addSideBarClass, removeSideBarClass } from '../../../helpers/pages';
import { isDefined } from '../../../helpers/types';
import { InstalledPlugin, PluginType } from '../../../types/plugins';
import { Dict } from '../../../types/types';
import { getUrlsList } from '../navTreeUtils';
import getPages from '../pages';
import '../styles.css';
import { DocumentationEntry } from '../utils';
import Sidebar from './Sidebar';

interface Props {
  params: { splat?: string };
  location: { hash: string };
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

    const tree = (navigationTreeSonarQube as any).default as DocNavigationItem[];

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

  getLanguagePluginsDocumentation = async (tree: DocNavigationItem[]) => {
    const plugins = await getInstalledPlugins(PluginType.Bundled).catch(
      () => [] as InstalledPlugin[]
    );

    const pluginsWithDoc = await Promise.all(
      plugins.map(plugin => {
        if (plugin.documentationPath) {
          const matchArray = /^static\/(.*)/.exec(plugin.documentationPath);

          if (matchArray && matchArray.length > 1) {
            return getPluginStaticFileContent(plugin.key, matchArray[1]).then(
              content => ({ ...plugin, content }),
              () => undefined
            );
          }
        }

        return undefined;
      })
    );

    const regex = new RegExp(`/${LANGUAGES_BASE_URL}/\\w+/$`);
    const overridablePaths = getUrlsList(tree).filter(
      path => regex.test(path) && path !== `/${LANGUAGES_BASE_URL}/overview/`
    );

    const parsedContent: Dict<ParsedContent> = {};

    pluginsWithDoc.filter(isDefined).forEach(plugin => {
      const parsed = separateFrontMatter(plugin.content);

      if (plugin.issueTrackerUrl) {
        // Inject issue tracker link
        let issueTrackerLink = '## Issue Tracker';
        issueTrackerLink += '\r\n';
        issueTrackerLink += `Check the [issue tracker](${plugin.issueTrackerUrl}) for this language.`;
        parsed.content = `${parsed.content}\r\n${issueTrackerLink}`;
      }

      if (
        parsed?.frontmatter?.key &&
        overridablePaths.includes(`/${LANGUAGES_BASE_URL}/${parsed.frontmatter.key}/`)
      ) {
        parsedContent[`${LANGUAGES_BASE_URL}/${parsed.frontmatter.key}`] = parsed;
      }
    });

    return parsedContent;
  };

  render() {
    const { loading, pages, tree } = this.state;
    const {
      params: { splat = '' },
      location: { hash }
    } = this.props;

    if (loading) {
      return (
        <div className="page page-limited">
          <DeferredSpinner />
        </div>
      );
    }

    const page = pages.find(p => p.url === `/${splat}`);
    const mainTitle = translate('documentation.page_title.sonarqube');
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
        <Helmet
          defer={false}
          title={isIndex || !page.title ? mainTitle : `${page.title} | ${mainTitle}`}>
          <meta content="noindex nofollow" name="robots" />
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
                scrollToHref={hash}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
}
