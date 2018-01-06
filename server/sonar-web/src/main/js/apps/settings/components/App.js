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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import PageHeader from './PageHeader';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import AllCategoriesList from './AllCategoriesList';
import WildcardsHelp from './WildcardsHelp';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

/*::
type Props = {
  component?: { key: string },
  defaultCategory: ?string,
  fetchSettings(componentKey: ?string): Promise<*>,
  location: { query: {} }
};
*/

/*::
type State = {
  loaded: boolean
};
*/

export default class App extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = { loaded: false };

  componentDidMount() {
    const componentKey = this.props.component ? this.props.component.key : null;
    this.props.fetchSettings(componentKey).then(() => this.setState({ loaded: true }));
  }

  componentDidUpdate(prevProps /*: Props*/) {
    if (prevProps.component !== this.props.component) {
      const componentKey = this.props.component ? this.props.component.key : null;
      this.props.fetchSettings(componentKey);
    }
  }

  render() {
    if (!this.state.loaded) {
      return null;
    }

    const { query } = this.props.location;
    const selectedCategory = query.category || this.props.defaultCategory;

    return (
      <div id="settings-page" className="page page-limited">
        <Helmet title={translate('settings.page')} />

        <PageHeader component={this.props.component} />

        <div className="side-tabs-layout settings-layout">
          <div className="side-tabs-side">
            <AllCategoriesList
              component={this.props.component}
              selectedCategory={selectedCategory}
              defaultCategory={this.props.defaultCategory}
            />
          </div>
          <div className="side-tabs-main">
            <CategoryDefinitionsList component={this.props.component} category={selectedCategory} />
            {selectedCategory === 'exclusions' && <WildcardsHelp />}
          </div>
        </div>
      </div>
    );
  }
}
