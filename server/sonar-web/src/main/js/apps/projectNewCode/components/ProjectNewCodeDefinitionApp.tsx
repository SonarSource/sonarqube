/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import {
  getNewCodeDefinition,
  resetNewCodeDefinition,
  setNewCodeDefinition,
} from '../../../api/newCodeDefinition';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
import Spinner from '../../../components/ui/Spinner';
import { isBranch, sortBranches } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import {
  DEFAULT_NEW_CODE_DEFINITION_TYPE,
  getNumberOfDaysDefaultValue,
} from '../../../helpers/new-code-definition';
import { withBranchLikes } from '../../../queries/branch';
import { AppState } from '../../../types/appstate';
import { Branch, BranchLike } from '../../../types/branch-like';
import { Feature } from '../../../types/features';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Component } from '../../../types/types';
import '../styles.css';
import { getSettingValue } from '../utils';
import AppHeader from './AppHeader';
import BranchList from './BranchList';
import ProjectNewCodeDefinitionSelector from './ProjectNewCodeDefinitionSelector';

interface Props extends WithAvailableFeaturesProps {
  branchLike: Branch;
  branchLikes: BranchLike[];
  component: Component;
  appState: AppState;
}

interface State {
  analysis?: string;
  branchList: Branch[];
  newCodeDefinitionType?: NewCodeDefinitionType;
  newCodeDefinitionValue?: string;
  previousNonCompliantValue?: string;
  projectNcdUpdatedAt?: number;
  numberOfDays: string;
  globalNewCodeDefinition?: NewCodeDefinition;
  isChanged: boolean;
  loading: boolean;
  overrideGlobalNewCodeDefinition?: boolean;
  referenceBranch?: string;
  saving: boolean;
  selectedNewCodeDefinitionType?: NewCodeDefinitionType;
  success?: boolean;
}

class ProjectNewCodeDefinitionApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    branchList: [],
    numberOfDays: getNumberOfDaysDefaultValue(),
    isChanged: false,
    loading: true,
    saving: false,
  };

  // We use debounce as we could have multiple save in less that 3sec.
  resetSuccess = debounce(() => this.setState({ success: undefined }), 3000);

  componentDidMount() {
    this.mounted = true;
    this.fetchLeakPeriodSetting();
    this.sortAndFilterBranches(this.props.branchLikes);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.branchLikes !== this.props.branchLikes) {
      this.sortAndFilterBranches(this.props.branchLikes);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getUpdatedState(params: {
    newCodeDefinitionType?: NewCodeDefinitionType;
    newCodeDefinitionValue?: string;
    globalNewCodeDefinition: NewCodeDefinition;
    previousNonCompliantValue?: string;
    projectNcdUpdatedAt?: number;
  }) {
    const {
      newCodeDefinitionType,
      newCodeDefinitionValue,
      globalNewCodeDefinition,
      previousNonCompliantValue,
      projectNcdUpdatedAt,
    } = params;
    const { referenceBranch } = this.state;

    const defaultDays = getNumberOfDaysDefaultValue(globalNewCodeDefinition);

    return {
      loading: false,
      newCodeDefinitionType,
      newCodeDefinitionValue,
      previousNonCompliantValue,
      projectNcdUpdatedAt,
      globalNewCodeDefinition,
      isChanged: false,
      selectedNewCodeDefinitionType: newCodeDefinitionType ?? globalNewCodeDefinition.type,
      overrideGlobalNewCodeDefinition: Boolean(newCodeDefinitionType),
      numberOfDays:
        (newCodeDefinitionType === NewCodeDefinitionType.NumberOfDays && newCodeDefinitionValue) ||
        defaultDays,
      analysis:
        (newCodeDefinitionType === NewCodeDefinitionType.SpecificAnalysis &&
          newCodeDefinitionValue) ||
        '',
      referenceBranch:
        (newCodeDefinitionType === NewCodeDefinitionType.ReferenceBranch &&
          newCodeDefinitionValue) ||
        referenceBranch,
    };
  }

  sortAndFilterBranches(branchLikes: BranchLike[] = []) {
    const branchList = sortBranches(branchLikes.filter(isBranch));
    this.setState({ branchList, referenceBranch: branchList[0]?.name });
  }

  fetchLeakPeriodSetting() {
    const { branchLike, component } = this.props;

    this.setState({ loading: true });

    Promise.all([
      getNewCodeDefinition(),
      getNewCodeDefinition({
        branch: this.props.hasFeature(Feature.BranchSupport) ? undefined : branchLike?.name,
        project: component.key,
      }),
    ]).then(
      ([globalNewCodeDefinition, setting]) => {
        if (this.mounted) {
          if (!globalNewCodeDefinition.type) {
            globalNewCodeDefinition = { type: DEFAULT_NEW_CODE_DEFINITION_TYPE };
          }
          const newCodeDefinitionValue = setting.value;
          const newCodeDefinitionType = setting.inherited
            ? undefined
            : setting.type || DEFAULT_NEW_CODE_DEFINITION_TYPE;

          this.setState(
            this.getUpdatedState({
              globalNewCodeDefinition,
              newCodeDefinitionType,
              newCodeDefinitionValue,
              previousNonCompliantValue: setting.previousNonCompliantValue,
              projectNcdUpdatedAt: setting.updatedAt,
            })
          );
        }
      },
      () => {
        this.setState({ loading: false });
      }
    );
  }

  resetSetting = () => {
    this.setState({ saving: true });
    resetNewCodeDefinition({ project: this.props.component.key }).then(
      () => {
        this.setState({
          saving: false,
          newCodeDefinitionType: undefined,
          isChanged: false,
          selectedNewCodeDefinitionType: undefined,
          success: true,
        });
        this.resetSuccess();
      },
      () => {
        this.setState({ saving: false });
      }
    );
  };

  handleSelectDays = (days: string) => this.setState({ numberOfDays: days, isChanged: true });

  handleSelectReferenceBranch = (referenceBranch: string) => {
    this.setState({ referenceBranch, isChanged: true });
  };

  handleCancel = () =>
    this.setState(
      ({
        globalNewCodeDefinition = { type: DEFAULT_NEW_CODE_DEFINITION_TYPE },
        newCodeDefinitionType,
        newCodeDefinitionValue,
      }) =>
        this.getUpdatedState({
          globalNewCodeDefinition,
          newCodeDefinitionType,
          newCodeDefinitionValue,
        })
    );

  handleSelectSetting = (selectedNewCodeDefinitionType?: NewCodeDefinitionType) => {
    this.setState((currentState) => ({
      selectedNewCodeDefinitionType,
      isChanged: selectedNewCodeDefinitionType !== currentState.selectedNewCodeDefinitionType,
    }));
  };

  handleToggleSpecificSetting = (overrideGlobalNewCodeDefinition: boolean) =>
    this.setState((currentState) => ({
      overrideGlobalNewCodeDefinition,
      isChanged: currentState.overrideGlobalNewCodeDefinition !== overrideGlobalNewCodeDefinition,
    }));

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const { component } = this.props;
    const {
      numberOfDays,
      selectedNewCodeDefinitionType: type,
      referenceBranch,
      overrideGlobalNewCodeDefinition,
    } = this.state;

    if (!overrideGlobalNewCodeDefinition) {
      this.resetSetting();
      return;
    }

    const value = getSettingValue({ type, numberOfDays, referenceBranch });

    if (type) {
      this.setState({ saving: true });
      setNewCodeDefinition({
        project: component.key,
        type,
        value,
      }).then(
        () => {
          this.setState({
            saving: false,
            newCodeDefinitionType: type,
            newCodeDefinitionValue: value || undefined,
            previousNonCompliantValue: undefined,
            projectNcdUpdatedAt: Date.now(),
            isChanged: false,
            success: true,
          });
          this.resetSuccess();
        },
        () => {
          this.setState({ saving: false });
        }
      );
    }
  };

  render() {
    const { appState, component, branchLike } = this.props;
    const {
      analysis,
      branchList,
      newCodeDefinitionType,
      numberOfDays,
      previousNonCompliantValue,
      projectNcdUpdatedAt,
      globalNewCodeDefinition,
      isChanged,
      loading,
      newCodeDefinitionValue,
      overrideGlobalNewCodeDefinition,
      referenceBranch,
      saving,
      selectedNewCodeDefinitionType,
      success,
    } = this.state;
    const branchSupportEnabled = this.props.hasFeature(Feature.BranchSupport);

    return (
      <>
        <Suggestions suggestions="project_baseline" />
        <Helmet defer={false} title={translate('project_baseline.page')} />
        <div className="page page-limited">
          <AppHeader canAdmin={!!appState.canAdmin} />
          <Spinner loading={loading} />

          {!loading && (
            <div className="panel-white project-baseline">
              {branchSupportEnabled && <h2>{translate('project_baseline.default_setting')}</h2>}

              {globalNewCodeDefinition && overrideGlobalNewCodeDefinition !== undefined && (
                <ProjectNewCodeDefinitionSelector
                  analysis={analysis}
                  branch={branchLike}
                  branchList={branchList}
                  branchesEnabled={branchSupportEnabled}
                  canAdmin={appState.canAdmin}
                  component={component.key}
                  newCodeDefinitionType={newCodeDefinitionType}
                  newCodeDefinitionValue={newCodeDefinitionValue}
                  days={numberOfDays}
                  previousNonCompliantValue={previousNonCompliantValue}
                  projectNcdUpdatedAt={projectNcdUpdatedAt}
                  globalNewCodeDefinition={globalNewCodeDefinition}
                  isChanged={isChanged}
                  onCancel={this.handleCancel}
                  onSelectDays={this.handleSelectDays}
                  onSelectReferenceBranch={this.handleSelectReferenceBranch}
                  onSelectSetting={this.handleSelectSetting}
                  onSubmit={this.handleSubmit}
                  onToggleSpecificSetting={this.handleToggleSpecificSetting}
                  overrideGlobalNewCodeDefinition={overrideGlobalNewCodeDefinition}
                  referenceBranch={referenceBranch}
                  saving={saving}
                  selectedNewCodeDefinitionType={selectedNewCodeDefinitionType}
                />
              )}

              <div className={classNames('spacer-top', { invisible: saving || !success })}>
                <span className="text-success">
                  <AlertSuccessIcon className="spacer-right" />
                  {translate('settings.state.saved')}
                </span>
              </div>
              {globalNewCodeDefinition && branchSupportEnabled && (
                <div className="huge-spacer-top branch-baseline-selector">
                  <hr />
                  <h2>{translate('project_baseline.configure_branches')}</h2>
                  <BranchList
                    branchList={branchList}
                    component={component}
                    inheritedSetting={
                      newCodeDefinitionType
                        ? {
                            type: newCodeDefinitionType,
                            value: newCodeDefinitionValue,
                          }
                        : globalNewCodeDefinition
                    }
                    globalNewCodeDefinition={globalNewCodeDefinition}
                  />
                </div>
              )}
            </div>
          )}
        </div>
      </>
    );
  }
}

export default withComponentContext(
  withAvailableFeatures(withAppStateContext(withBranchLikes(ProjectNewCodeDefinitionApp)))
);
