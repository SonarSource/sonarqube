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
import * as classNames from 'classnames';
import { keyBy } from 'lodash';
import * as React from 'react';
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getRepositories } from '../../../api/alm-integration';
import { isPaidOrganization } from '../../../helpers/organizations';
import UpgradeOrganizationBox from '../components/UpgradeOrganizationBox';
import AlmRepositoryItem from './AlmRepositoryItem';
import SetupProjectBox from './SetupProjectBox';

interface Props {
  almApplication: T.AlmApplication;
  onOrganizationUpgrade: () => void;
  onProjectCreate: (projectKeys: string[], organization: string) => void;
  organization: T.Organization;
}

type SelectedRepositories = T.Dict<T.AlmRepository | undefined>;

interface State {
  checkAllRepositories: boolean;
  highlight: boolean;
  loading: boolean;
  repositories: T.AlmRepository[];
  search: string;
  selectedRepositories: SelectedRepositories;
  successfullyUpgraded: boolean;
}

export default class RemoteRepositories extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    checkAllRepositories: false,
    highlight: false,
    loading: true,
    repositories: [],
    search: '',
    selectedRepositories: {},
    successfullyUpgraded: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchRepositories();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.organization.key !== this.props.organization.key) {
      this.setState({ loading: true, selectedRepositories: {} });
      this.fetchRepositories();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRepositories = () => {
    const { organization } = this.props;
    return getRepositories({ organization: organization.key }).then(
      ({ repositories }) => {
        if (this.mounted) {
          this.setState({ loading: false, repositories });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  filterBySearch = (search: String) => (repo: T.AlmRepository) => {
    return repo.label.toLowerCase().includes(search.toLowerCase());
  };

  handleHighlightUpgradeBox = (highlight: boolean) => {
    this.setState({ highlight });
  };

  handleOrganizationUpgrade = () => {
    this.props.onOrganizationUpgrade();
    if (this.mounted) {
      this.setState({ successfullyUpgraded: true });
    }
  };

  handleProvisionFail = () => {
    return this.fetchRepositories().then(() => {
      if (this.mounted) {
        this.setState(({ repositories, selectedRepositories }) => {
          const updateSelectedRepositories: SelectedRepositories = {};
          Object.keys(selectedRepositories).forEach(installationKey => {
            const newRepository = repositories.find(r => r.installationKey === installationKey);
            if (newRepository && !newRepository.linkedProjectKey) {
              updateSelectedRepositories[newRepository.installationKey] = newRepository;
            }
          });
          return { selectedRepositories: updateSelectedRepositories };
        });
      }
    });
  };

  handleSearch = (search: string) => {
    this.setState({ search, checkAllRepositories: false, selectedRepositories: {} });
  };

  onCheckAllRepositories = () => {
    this.setState(({ checkAllRepositories, repositories, search }) => {
      const { organization } = this.props;

      const isPaidOrg = isPaidOrganization(organization);
      const filterByPlan = (repo: T.AlmRepository) => (isPaidOrg ? true : !repo.private);
      const filterByImportable = (repo: T.AlmRepository) => !repo.linkedProjectKey;

      const nextState = {
        selectedRepositories: {},
        checkAllRepositories: !checkAllRepositories
      };

      if (nextState.checkAllRepositories) {
        const validRepositories = repositories.filter(
          repo =>
            this.filterBySearch(search)(repo) && filterByPlan(repo) && filterByImportable(repo)
        );
        nextState.selectedRepositories = keyBy(validRepositories, 'installationKey');
      }

      return nextState;
    });
  };

  toggleRepository = (repository: T.AlmRepository) => {
    this.setState(({ selectedRepositories }) => ({
      selectedRepositories: {
        ...selectedRepositories,
        [repository.installationKey]: selectedRepositories[repository.installationKey]
          ? undefined
          : repository
      }
    }));
  };

  render() {
    const { highlight, loading, repositories, search, selectedRepositories } = this.state;
    const { almApplication, organization } = this.props;
    const isPaidOrg = isPaidOrganization(organization);
    const hasPrivateRepositories = repositories.some(repository => Boolean(repository.private));
    const showSearchBox = repositories.length > 5;
    const showCheckAll = repositories.length > 1;
    const showUpgradebox =
      !isPaidOrg && hasPrivateRepositories && organization.actions && organization.actions.admin;
    const filteredRepositories = repositories.filter(this.filterBySearch(search));

    return (
      <div className="create-project">
        <div className="flex-1 huge-spacer-right">
          <div className="spacer-bottom create-project-actions">
            <div>
              {showCheckAll && (
                <Checkbox
                  checked={this.state.checkAllRepositories}
                  disabled={filteredRepositories.length === 0}
                  onCheck={this.onCheckAllRepositories}>
                  {translate('onboarding.create_project.select_all_repositories')}
                </Checkbox>
              )}
            </div>
            {showSearchBox && (
              <SearchBox
                minLength={1}
                onChange={this.handleSearch}
                placeholder={translate('search.search_for_repositories')}
                value={this.state.search}
              />
            )}
          </div>

          {this.state.successfullyUpgraded && (
            <Alert variant="success">
              {translateWithParameters(
                'onboarding.create_project.subscribtion_success_x',
                organization.name
              )}
            </Alert>
          )}
          <DeferredSpinner loading={loading}>
            <ul>
              {filteredRepositories.length === 0 && (
                <li className="big-spacer-top note">
                  {showUpgradebox
                    ? translateWithParameters('no_results_for_x', search)
                    : translate('onboarding.create_project.no_repositories')}
                </li>
              )}
              {filteredRepositories.map(repo => (
                <AlmRepositoryItem
                  disabled={Boolean(repo.private && !isPaidOrg)}
                  highlightUpgradeBox={this.handleHighlightUpgradeBox}
                  identityProvider={almApplication}
                  key={repo.installationKey}
                  repository={repo}
                  selected={Boolean(selectedRepositories[repo.installationKey])}
                  toggleRepository={this.toggleRepository}
                />
              ))}
            </ul>
          </DeferredSpinner>
        </div>
        {organization && (
          <div className={classNames({ 'create-project-side-with-search': showSearchBox })}>
            <div className="create-project-side-sticky">
              <SetupProjectBox
                onProjectCreate={this.props.onProjectCreate}
                onProvisionFail={this.handleProvisionFail}
                organization={organization}
                selectedRepositories={Object.keys(selectedRepositories)
                  .map(r => selectedRepositories[r])
                  .filter(isDefined)}
              />
              {showUpgradebox && (
                <UpgradeOrganizationBox
                  className={classNames({ highlight })}
                  onOrganizationUpgrade={this.handleOrganizationUpgrade}
                  organization={organization}
                />
              )}
            </div>
          </div>
        )}
      </div>
    );
  }
}
