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
import * as React from 'react';
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import * as navigationTreeSonarQube from 'Docs/../static/SonarQubeNavigationTree.json';
import * as navigationTreeSonarCloud from 'Docs/../static/SonarCloudNavigationTree.json';
import Sidebar from './Sidebar';
import getPages from '../pages';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import NotFound from '../../../app/components/NotFound';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import DocMarkdownBlock from '../../../components/docs/DocMarkdownBlock';
import { translate } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import { addSideBarClass, removeSideBarClass } from '../../../helpers/pages';
import { DocsNavigationItem } from '../utils';
import '../styles.css';

interface Props {
  params: { splat?: string };
}

export default class App extends React.PureComponent<Props> {
  mounted = false;
  pages = getPages();

  componentDidMount() {
    addSideBarClass();
  }

  componentWillUnmount() {
    removeSideBarClass();
  }

  render() {
    const tree = isSonarCloud()
      ? ((navigationTreeSonarCloud as any).default as DocsNavigationItem[])
      : ((navigationTreeSonarQube as any).default as DocsNavigationItem[]);
    const { splat = '' } = this.props.params;
    const page = this.pages.find(p => p.url === '/' + splat);
    const mainTitle = translate('documentation.page_title');

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

    const isIndex = splat === 'index';

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
                  <Sidebar navigation={tree} pages={this.pages} splat={splat} />
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
                displayH1={true}
                stickyToc={true}
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
}
