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
import { connect } from 'react-redux';
import { WithRouterProps } from 'react-router';
import AllCategoriesList from './AllCategoriesList';
import CategoryDefinitionsList from './CategoryDefinitionsList';
import PageHeader from './PageHeader';
import WildcardsHelp from './WildcardsHelp';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { fetchSettings } from '../store/actions';
import { getSettingsAppDefaultCategory, Store } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import '../styles.css';
import '../side-tabs.css';

interface Props {
  component?: T.Component;
  defaultCategory: string;
  fetchSettings(component?: string): Promise<void>;
}

interface State {
  loading: boolean;
}

export class App extends React.PureComponent<Props & WithRouterProps, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchSettings();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchSettings();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSettings = () => {
    const { component } = this.props;
    this.props.fetchSettings(component && component.key).then(this.stopLoading, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    if (this.state.loading) {
      return null;
    }

    const { query } = this.props.location;
    const selectedCategory = query.category || this.props.defaultCategory;

    return (
      <div className="page page-limited" id="settings-page">
        <Suggestions suggestions="settings" />
        <Helmet title={translate('settings.page')} />

        <PageHeader component={this.props.component} />

        <div className="side-tabs-layout settings-layout">
          <div className="side-tabs-side">
            <AllCategoriesList
              component={this.props.component}
              defaultCategory={this.props.defaultCategory}
              selectedCategory={selectedCategory}
            />
          </div>
          <div className="side-tabs-main">
            <CategoryDefinitionsList category={selectedCategory} component={this.props.component} />
            {selectedCategory === 'exclusions' && <WildcardsHelp />}
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  defaultCategory: getSettingsAppDefaultCategory(state)
});

const mapDispatchToProps = { fetchSettings: fetchSettings as any };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
