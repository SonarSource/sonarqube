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

import styled from '@emotion/styled';
import {
  Button,
  ButtonVariety,
  Checkbox,
  Heading,
  IconCheckCircle,
  IconError,
  IconInfo,
  Link,
  RadioButtonGroup,
  Spinner,
  Text,
} from '@sonarsource/echoes-react';
import { MutationStatus } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import React, { useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  BasicSeparator,
  HighlightedSection,
  Note,
  themeColor,
  UnorderedList,
} from '~design-system';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { searchProjects } from '../../../api/components';
import { SuggestionServiceStatusCheckResponse } from '../../../api/fix-suggestions';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import DocumentationLink from '../../../components/common/DocumentationLink';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams,
} from '../../../components/controls/SelectList';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { getAiCodeFixTermsOfServiceUrl } from '../../../helpers/urls';
import {
  useCheckServiceMutation,
  useRemoveCodeSuggestionsCache,
  useUpdateFeatureEnablementMutation,
} from '../../../queries/fix-suggestions';
import { useGetValueQuery } from '../../../queries/settings';
import { Feature } from '../../../types/features';
import { AiCodeFixFeatureEnablement } from '../../../types/fix-suggestions';
import { SettingsKey } from '../../../types/settings';
import PromotedSection from '../../overview/branches/PromotedSection';

interface Props extends WithAvailableFeaturesProps {}

const AI_CODE_FIX_SETTING_KEY = SettingsKey.CodeSuggestion;

function AiCodeFixAdmin({ hasFeature }: Readonly<Props>) {
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
  const {
    mutate: checkService,
    isIdle,
    isPending: isServiceCheckPending,
    status,
    error,
    data,
  } = useCheckServiceMutation();

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

  if (!hasFeature(Feature.FixSuggestions)) {
    return null;
  }

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
        <PromotedSection
          content={
            <>
              <p>{translate('property.aicodefix.admin.promoted_section.content1')}</p>
              <p className="sw-mt-2">
                {translate('property.aicodefix.admin.promoted_section.content2')}
              </p>
            </>
          }
          title={translate('property.aicodefix.admin.promoted_section.title')}
        />
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
      <div className="sw-flex-col sw-w-abs-600 sw-p-6">
        <HighlightedSection className="sw-items-start">
          <Heading as="h3" hasMarginBottom>
            {translate('property.aicodefix.admin.serviceCheck.title')}
          </Heading>
          <p>{translate('property.aicodefix.admin.serviceCheck.description1')}</p>
          <DocumentationLink to={DocLink.AiCodeFixEnabling}>
            {translate('property.aicodefix.admin.serviceCheck.learnMore')}
          </DocumentationLink>
          <p>{translate('property.aicodefix.admin.serviceCheck.description2')}</p>
          <Button
            className="sw-mt-4"
            variety={ButtonVariety.Default}
            onClick={() => checkService()}
            isDisabled={isServiceCheckPending}
          >
            {translate('property.aicodefix.admin.serviceCheck.action')}
          </Button>
          {!isIdle && (
            <div>
              <BasicSeparator className="sw-my-4" />
              <ServiceCheckResultView data={data} error={error} status={status} />
            </div>
          )}
        </HighlightedSection>
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

interface ServiceCheckResultViewProps {
  data: SuggestionServiceStatusCheckResponse | undefined;
  error: AxiosError | null;
  status: MutationStatus;
}

function ServiceCheckResultView({ data, error, status }: Readonly<ServiceCheckResultViewProps>) {
  switch (status) {
    case 'pending':
      return <Spinner label={translate('property.aicodefix.admin.serviceCheck.spinner.label')} />;
    case 'error':
      return (
        <ErrorMessage
          text={`${translate('property.aicodefix.admin.serviceCheck.result.requestError')} ${error?.status ?? 'No status'}`}
        />
      );
    case 'success':
      return ServiceCheckValidResponseView(data);
  }
  // normally unreachable
  throw Error(`Unexpected response from the service status check, received ${status}`);
}

function ServiceCheckValidResponseView(data: SuggestionServiceStatusCheckResponse | undefined) {
  switch (data?.status) {
    case 'SUCCESS':
      return (
        <SuccessMessage text={translate('property.aicodefix.admin.serviceCheck.result.success')} />
      );
    case 'TIMEOUT':
    case 'CONNECTION_ERROR':
      return (
        <div className="sw-flex">
          <IconError className="sw-mr-1" color="echoes-color-icon-danger" />
          <div className="sw-flex-col">
            <ErrorLabel
              text={translate('property.aicodefix.admin.serviceCheck.result.unresponsive.message')}
            />
            <p className="sw-mt-4">
              <ErrorLabel
                text={translate(
                  'property.aicodefix.admin.serviceCheck.result.unresponsive.causes.title',
                )}
              />
            </p>
            <UnorderedList className="sw-ml-8" ticks>
              <ErrorListItem className="sw-mb-2">
                <ErrorLabel
                  text={translate(
                    'property.aicodefix.admin.serviceCheck.result.unresponsive.causes.1',
                  )}
                />
              </ErrorListItem>
              <ErrorListItem>
                <ErrorLabel
                  text={translate(
                    'property.aicodefix.admin.serviceCheck.result.unresponsive.causes.2',
                  )}
                />
              </ErrorListItem>
            </UnorderedList>
          </div>
        </div>
      );
    case 'UNAUTHORIZED':
      return (
        <ErrorMessage
          text={translate('property.aicodefix.admin.serviceCheck.result.unauthorized')}
        />
      );
    case 'SERVICE_ERROR':
      return (
        <ErrorMessage
          text={translate('property.aicodefix.admin.serviceCheck.result.serviceError')}
        />
      );
    default:
      return (
        <ErrorMessage
          text={`${translate('property.aicodefix.admin.serviceCheck.result.unknown')} ${data?.status ?? 'no status returned from the service'}`}
        />
      );
  }
}

function ErrorMessage({ text }: Readonly<TextProps>) {
  return (
    <div className="sw-flex">
      <IconError className="sw-mr-1" color="echoes-color-icon-danger" />
      <ErrorLabel text={text} />
    </div>
  );
}

function ErrorLabel({ text }: Readonly<TextProps>) {
  return <Text colorOverride="echoes-color-text-danger">{text}</Text>;
}

function SuccessMessage({ text }: Readonly<TextProps>) {
  return (
    <div className="sw-flex">
      <IconCheckCircle className="sw-mr-1" color="echoes-color-icon-success" />
      <Text colorOverride="echoes-color-text-success">{text}</Text>
    </div>
  );
}

const ErrorListItem = styled.li`
  ::marker {
    color: ${themeColor('errorText')};
  }
`;

interface TextProps {
  /** The text to display inside the component */
  text: string;
}

export default withAvailableFeatures(AiCodeFixAdmin);
