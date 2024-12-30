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

import {
  Button,
  ButtonVariety,
  Checkbox,
  Heading,
  IconInfo,
  Link,
  RadioButtonGroup,
  Text,
} from '@sonarsource/echoes-react';
import React, { useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import { Note } from '~design-system';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { searchProjects } from '../../../../api/components';
import withAvailableFeatures from '../../../../app/components/available-features/withAvailableFeatures';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams,
} from '../../../../components/controls/SelectList';
import { translate } from '../../../../helpers/l10n';
import { getAiCodeFixTermsOfServiceUrl } from '../../../../helpers/urls';
import {
  useRemoveCodeSuggestionsCache,
  useUpdateFeatureEnablementMutation,
} from '../../../../queries/fix-suggestions';
import { useGetValueQuery } from '../../../../queries/settings';
import { AiCodeFixFeatureEnablement } from '../../../../types/fix-suggestions';
import { SettingsKey } from '../../../../types/settings';
import PromotedSection from '../../../overview/branches/PromotedSection';

const AI_CODE_FIX_SETTING_KEY = SettingsKey.CodeSuggestion;

interface AiCodeFixEnablementFormProps {
  isEarlyAccess?: boolean;
}

function AiCodeFixEnablementForm({ isEarlyAccess }: Readonly<AiCodeFixEnablementFormProps>) {
  const { data: aiCodeFixSetting } = useGetValueQuery({
    key: AI_CODE_FIX_SETTING_KEY,
  });
  const removeCodeSuggestionsCache = useRemoveCodeSuggestionsCache();

  const initialAiCodeFixEnablement =
    (aiCodeFixSetting?.value as AiCodeFixFeatureEnablement) || AiCodeFixFeatureEnablement.disabled;

  const [savedAiCodeFixEnablement, setSavedAiCodeFixEnablement] = React.useState(
    initialAiCodeFixEnablement,
  );
  const [currentAiCodeFixEnablement, setCurrentAiCodeFixEnablement] =
    React.useState(savedAiCodeFixEnablement);

  const { mutate: updateFeatureEnablement } = useUpdateFeatureEnablementMutation();
  const [changedProjects, setChangedProjects] = React.useState<Map<string, boolean>>(new Map());

  useEffect(() => {
    setSavedAiCodeFixEnablement(initialAiCodeFixEnablement);
  }, [initialAiCodeFixEnablement]);

  useEffect(() => {
    setCurrentAiCodeFixEnablement(savedAiCodeFixEnablement);
  }, [savedAiCodeFixEnablement]);

  const [currentSearchResults, setCurrentSearchResults] = React.useState<ProjectSearchResult>();
  const [currentTabItems, setCurrentTabItems] = React.useState<ProjectItem[]>([]);

  const handleSave = () => {
    updateFeatureEnablement(
      {
        enablement: currentAiCodeFixEnablement,
        changes: {
          enabledProjectKeys: [...changedProjects]
            .filter(([_, enabled]) => enabled)
            .map(([project]) => project),
          disabledProjectKeys: [...changedProjects]
            .filter(([_, enabled]) => !enabled)
            .map(([project]) => project),
        },
      },
      {
        onSuccess: () => {
          removeCodeSuggestionsCache();
          const savedChanges = changedProjects;
          setChangedProjects(new Map());
          setSavedAiCodeFixEnablement(currentAiCodeFixEnablement);
          if (currentSearchResults) {
            // some items might not be in the right tab if they were toggled just before saving, we need to refresh the view
            updateItemsWithSearchResult(currentSearchResults, savedChanges);
          }
        },
      },
    );
  };

  const handleCancel = () => {
    setCurrentAiCodeFixEnablement(savedAiCodeFixEnablement);
    setChangedProjects(new Map());
    if (currentSearchResults) {
      // some items might have moved to another tab than the current one, we need to refresh the view
      updateItemsWithSearchResult(currentSearchResults, new Map());
    }
  };

  const renderProjectElement = (projectKey: string): React.ReactNode => {
    const project = currentTabItems.find((project) => project.key === projectKey);
    return (
      <div>
        {project === undefined ? (
          projectKey
        ) : (
          <>
            {project.name}
            <br />
            <Note>{project.key}</Note>
          </>
        )}
      </div>
    );
  };

  const onProjectSelected = (projectKey: string) => {
    const newChangedProjects = new Map(changedProjects);
    newChangedProjects.set(projectKey, true);
    setChangedProjects(newChangedProjects);
    const project = currentTabItems.find((project) => project.key === projectKey);
    if (project) {
      project.selected = true;
      setCurrentTabItems([...currentTabItems]);
    }
    return Promise.resolve();
  };

  const onProjectUnselected = (projectKey: string) => {
    const newChangedProjects = new Map(changedProjects);
    newChangedProjects.set(projectKey, false);
    setChangedProjects(newChangedProjects);
    const project = currentTabItems.find((project) => project.key === projectKey);
    if (project) {
      project.selected = false;
      setCurrentTabItems([...currentTabItems]);
    }
    return Promise.resolve();
  };

  const onSearch = (searchParams: SelectListSearchParams) => {
    searchProjects({
      p: searchParams.page,
      filter: searchParams.query !== '' ? `query=${searchParams.query}` : undefined,
    })
      .then((response) => {
        const searchResults = {
          filter: searchParams.filter,
          projects: response.components.map((project) => {
            return {
              key: project.key,
              name: project.name,
              isAiCodeFixEnabled: project.isAiCodeFixEnabled === true,
            };
          }),
          totalCount: response.paging.total,
        };
        setCurrentSearchResults(searchResults);
        updateItemsWithSearchResult(searchResults, changedProjects);
      })
      .catch(throwGlobalError);
  };

  const updateItemsWithSearchResult = (
    searchResult: ProjectSearchResult,
    changedProjects: Map<string, boolean>,
  ) => {
    const { filter } = searchResult;
    setCurrentTabItems(
      searchResult.projects
        .filter(
          (project) =>
            filter === SelectListFilter.All ||
            (filter === SelectListFilter.Selected &&
              (changedProjects.has(project.key)
                ? changedProjects.get(project.key) === true
                : project.isAiCodeFixEnabled)) ||
            (filter === SelectListFilter.Unselected &&
              (changedProjects.has(project.key)
                ? !changedProjects.get(project.key)
                : !project.isAiCodeFixEnabled)),
        )
        .map((project) => {
          return {
            key: project.key,
            name: project.name,
            selected: changedProjects.has(project.key)
              ? changedProjects.get(project.key) === true
              : project.isAiCodeFixEnabled,
          };
        }),
    );
  };

  return (
    <div className="sw-flex">
      <div className="sw-flex-grow sw-p-6">
        <Heading as="h2" hasMarginBottom>
          {translate('property.aicodefix.admin.title')}
        </Heading>
        {isEarlyAccess && (
          <PromotedSection
            content={
              <>
                <p>{translate('property.aicodefix.admin.early_access.content1')}</p>
                <p className="sw-mt-2">
                  {translate('property.aicodefix.admin.early_access.content2')}
                </p>
              </>
            }
            title={translate('property.aicodefix.admin.early_access.title')}
          />
        )}
        <p>{translate('property.aicodefix.admin.description')}</p>
        <Checkbox
          className="sw-my-6"
          label={translate('property.aicodefix.admin.checkbox.label')}
          checked={currentAiCodeFixEnablement !== AiCodeFixFeatureEnablement.disabled}
          onCheck={() =>
            setCurrentAiCodeFixEnablement(
              currentAiCodeFixEnablement === AiCodeFixFeatureEnablement.disabled
                ? AiCodeFixFeatureEnablement.allProjects
                : AiCodeFixFeatureEnablement.disabled,
            )
          }
          helpText={
            <FormattedMessage
              id="property.aicodefix.admin.terms"
              defaultMessage={translate('property.aicodefix.admin.acceptTerm.label')}
              values={{
                terms: (
                  <Link shouldOpenInNewTab to={getAiCodeFixTermsOfServiceUrl()}>
                    {translate('property.aicodefix.admin.acceptTerm.terms')}
                  </Link>
                ),
              }}
            />
          }
        />
        <div className="sw-ml-6">
          {currentAiCodeFixEnablement !== AiCodeFixFeatureEnablement.disabled && (
            <RadioButtonGroup
              label={translate('property.aicodefix.admin.enable.title')}
              id="ai-code-fix-enablement"
              isRequired
              options={[
                {
                  helpText: translate('property.aicodefix.admin.enable.all.projects.help'),
                  label: translate('property.aicodefix.admin.enable.all.projects.label'),
                  value: AiCodeFixFeatureEnablement.allProjects,
                },
                {
                  helpText: translate('property.aicodefix.admin.enable.some.projects.help'),
                  label: translate('property.aicodefix.admin.enable.some.projects.label'),
                  value: AiCodeFixFeatureEnablement.someProjects,
                },
              ]}
              value={currentAiCodeFixEnablement}
              onChange={(enablement: AiCodeFixFeatureEnablement) =>
                setCurrentAiCodeFixEnablement(enablement)
              }
            />
          )}
          {currentAiCodeFixEnablement === AiCodeFixFeatureEnablement.someProjects && (
            <div className="sw-ml-6">
              <div className="sw-flex sw-mb-6">
                <IconInfo className="sw-mr-1" color="echoes-color-icon-info" />
                <Text>{translate('property.aicodefix.admin.enable.some.projects.note')}</Text>
              </div>
              <SelectList
                loading={false}
                elements={currentTabItems.map((project) => project.key)}
                elementsTotalCount={currentSearchResults?.totalCount}
                labelAll={translate('all')}
                labelSelected={translate('selected')}
                labelUnselected={translate('unselected')}
                needToReload={false}
                onSearch={onSearch}
                onSelect={onProjectSelected}
                onUnselect={onProjectUnselected}
                renderElement={renderProjectElement}
                selectedElements={currentTabItems.filter((p) => p.selected).map((u) => u.key)}
                withPaging
              />
            </div>
          )}
        </div>
        <div>
          <div className="sw-mt-6">
            <Button
              variety={ButtonVariety.Primary}
              isDisabled={
                currentAiCodeFixEnablement === savedAiCodeFixEnablement &&
                (currentAiCodeFixEnablement !== AiCodeFixFeatureEnablement.someProjects ||
                  changedProjects.size === 0)
              }
              onClick={() => {
                handleSave();
              }}
            >
              {translate('save')}
            </Button>
            <Button className="sw-ml-3" variety={ButtonVariety.Default} onClick={handleCancel}>
              {translate('cancel')}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

interface ProjectSearchResult {
  filter: SelectListFilter;
  projects: RemoteProject[];
  totalCount: number;
}

interface RemoteProject {
  isAiCodeFixEnabled: boolean;
  key: string;
  name: string;
}

interface ProjectItem {
  key: string;
  name: string;
  selected: boolean;
}

export default withAvailableFeatures(AiCodeFixEnablementForm);
