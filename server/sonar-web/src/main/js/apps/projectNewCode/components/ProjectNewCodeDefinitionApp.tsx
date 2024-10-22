/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { HeadingDark, LargeCenteredLayout, PageContentFontWrapper, Spinner } from '~design-system';
import { isBranch } from '~sonar-aligned/helpers/branch-like';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { sortBranches } from '../../../helpers/branch-like';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import {
  DEFAULT_NEW_CODE_DEFINITION_TYPE,
  getNumberOfDaysDefaultValue,
} from '../../../helpers/new-code-definition';
import { withBranchLikes } from '../../../queries/branch';
import {
  useNewCodeDefinitionMutation,
  useNewCodeDefinitionQuery,
} from '../../../queries/newCodeDefinition';
import { AppState } from '../../../types/appstate';
import { Branch, BranchLike } from '../../../types/branch-like';
import { Feature } from '../../../types/features';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Component } from '../../../types/types';
import { getSettingValue } from '../utils';
import AppHeader from './AppHeader';
import BranchList from './BranchList';
import ProjectNewCodeDefinitionSelector from './ProjectNewCodeDefinitionSelector';

interface ProjectNewCodeDefinitionAppProps extends WithAvailableFeaturesProps {
  appState: AppState;
  branchLike: Branch;
  branchLikes: BranchLike[];
  component: Component;
}

function ProjectNewCodeDefinitionApp(props: Readonly<ProjectNewCodeDefinitionAppProps>) {
  const { appState, component, branchLike, branchLikes, hasFeature } = props;

  const [isSpecificNewCodeDefinition, setIsSpecificNewCodeDefinition] = useState<boolean>();
  const [numberOfDays, setNumberOfDays] = useState(getNumberOfDaysDefaultValue());
  const [referenceBranch, setReferenceBranch] = useState<string | undefined>(undefined);
  const [specificAnalysis, setSpecificAnalysis] = useState<string | undefined>(undefined);

  const [selectedNewCodeDefinitionType, setSelectedNewCodeDefinitionType] =
    useState<NewCodeDefinitionType>(DEFAULT_NEW_CODE_DEFINITION_TYPE);

  const {
    data: globalNewCodeDefinition = { type: DEFAULT_NEW_CODE_DEFINITION_TYPE },
    isLoading: isGlobalNCDLoading,
  } = useNewCodeDefinitionQuery();

  const { data: projectNewCodeDefinition, isLoading: isProjectNCDLoading } =
    useNewCodeDefinitionQuery({
      branchName: hasFeature(Feature.BranchSupport) ? undefined : branchLike?.name,
      projectKey: component.key,
    });
  const { isPending: isSaving, mutate: postNewCodeDefinition } = useNewCodeDefinitionMutation();

  const branchList = useMemo(() => {
    return sortBranches(branchLikes.filter(isBranch));
  }, [branchLikes]);

  const isFormTouched = useMemo(() => {
    if (isSpecificNewCodeDefinition === undefined) {
      return false;
    }

    if (isSpecificNewCodeDefinition !== !projectNewCodeDefinition?.inherited) {
      return true;
    }

    if (!isSpecificNewCodeDefinition) {
      return false;
    }

    if (selectedNewCodeDefinitionType !== projectNewCodeDefinition?.type) {
      return true;
    }

    switch (selectedNewCodeDefinitionType) {
      case NewCodeDefinitionType.NumberOfDays:
        return numberOfDays !== String(projectNewCodeDefinition?.value);

      case NewCodeDefinitionType.ReferenceBranch:
        return referenceBranch !== projectNewCodeDefinition?.value;

      case NewCodeDefinitionType.SpecificAnalysis:
        return specificAnalysis !== projectNewCodeDefinition?.value;

      default:
        return false;
    }
  }, [
    isSpecificNewCodeDefinition,
    numberOfDays,
    projectNewCodeDefinition,
    referenceBranch,
    selectedNewCodeDefinitionType,
    specificAnalysis,
  ]);

  const defaultReferenceBranch = branchList[0]?.name;
  const isLoading = isGlobalNCDLoading || isProjectNCDLoading;
  const branchSupportEnabled = hasFeature(Feature.BranchSupport);

  const resetStatesFromProjectNewCodeDefinition = useCallback(() => {
    setIsSpecificNewCodeDefinition(
      projectNewCodeDefinition === undefined ? undefined : !projectNewCodeDefinition.inherited,
    );

    setSelectedNewCodeDefinitionType(
      projectNewCodeDefinition?.type ?? DEFAULT_NEW_CODE_DEFINITION_TYPE,
    );

    setNumberOfDays(getNumberOfDaysDefaultValue(globalNewCodeDefinition, projectNewCodeDefinition));

    setReferenceBranch(
      projectNewCodeDefinition?.type === NewCodeDefinitionType.ReferenceBranch
        ? projectNewCodeDefinition.value
        : defaultReferenceBranch,
    );

    setSpecificAnalysis(
      projectNewCodeDefinition?.type === NewCodeDefinitionType.SpecificAnalysis
        ? projectNewCodeDefinition.value
        : undefined,
    );
  }, [defaultReferenceBranch, globalNewCodeDefinition, projectNewCodeDefinition]);

  const onResetNewCodeDefinition = () => {
    postNewCodeDefinition({
      branch: hasFeature(Feature.BranchSupport) ? undefined : branchLike?.name,
      project: component.key,
      type: undefined,
    });
  };

  const onSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!isSpecificNewCodeDefinition) {
      onResetNewCodeDefinition();
      return;
    }

    const value = getSettingValue({
      type: selectedNewCodeDefinitionType,
      numberOfDays,
      referenceBranch,
    });

    if (selectedNewCodeDefinitionType) {
      postNewCodeDefinition({
        branch: hasFeature(Feature.BranchSupport) ? undefined : branchLike?.name,
        project: component.key,
        type: selectedNewCodeDefinitionType,
        value,
      });
    }
  };

  useEffect(() => {
    setReferenceBranch(defaultReferenceBranch);
  }, [defaultReferenceBranch]);

  useEffect(() => {
    resetStatesFromProjectNewCodeDefinition();
  }, [resetStatesFromProjectNewCodeDefinition]);

  return (
    <LargeCenteredLayout id="new-code-rules-page">
      <Suggestions suggestion={DocLink.NewCodeDefinition} />

      <Helmet defer={false} title={translate('project_baseline.page')} />

      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <AppHeader canAdmin={!!appState.canAdmin} />

        <Spinner loading={isLoading} />

        {!isLoading && (
          <div className="it__project-baseline">
            {globalNewCodeDefinition && isSpecificNewCodeDefinition !== undefined && (
              <ProjectNewCodeDefinitionSelector
                analysis={specificAnalysis}
                branch={branchLike}
                branchList={branchList}
                branchesEnabled={branchSupportEnabled}
                component={component.key}
                newCodeDefinitionType={projectNewCodeDefinition?.type}
                newCodeDefinitionValue={projectNewCodeDefinition?.value}
                days={numberOfDays}
                previousNonCompliantValue={projectNewCodeDefinition?.previousNonCompliantValue}
                projectNcdUpdatedAt={projectNewCodeDefinition?.updatedAt}
                globalNewCodeDefinition={globalNewCodeDefinition}
                isChanged={isFormTouched}
                onCancel={resetStatesFromProjectNewCodeDefinition}
                onSelectDays={setNumberOfDays}
                onSelectReferenceBranch={setReferenceBranch}
                onSelectSetting={setSelectedNewCodeDefinitionType}
                onSubmit={onSubmit}
                onToggleSpecificSetting={setIsSpecificNewCodeDefinition}
                overrideGlobalNewCodeDefinition={isSpecificNewCodeDefinition}
                referenceBranch={referenceBranch}
                saving={isSaving}
                selectedNewCodeDefinitionType={selectedNewCodeDefinitionType}
              />
            )}

            {globalNewCodeDefinition && branchSupportEnabled && (
              <div className="sw-mt-6">
                <HeadingDark as="h3" className="sw-mb-4">
                  {translate('project_baseline.configure_branches')}
                </HeadingDark>

                <BranchList
                  branchList={branchList}
                  component={component}
                  inheritedSetting={projectNewCodeDefinition ?? globalNewCodeDefinition}
                  globalNewCodeDefinition={globalNewCodeDefinition}
                />
              </div>
            )}
          </div>
        )}
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withComponentContext(
  withAvailableFeatures(withAppStateContext(withBranchLikes(ProjectNewCodeDefinitionApp))),
);
