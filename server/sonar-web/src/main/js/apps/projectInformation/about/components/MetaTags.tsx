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

import { Heading, Spinner } from '@sonarsource/echoes-react';
import { difference, without } from 'lodash';
import { useEffect, useState } from 'react';
import { addGlobalErrorMessage, MultiSelector, Tags } from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { searchProjectTags, setApplicationTags, setProjectTags } from '../../../../api/components';
import Tooltip from '../../../../components/controls/Tooltip';
import { PopupPlacement } from '../../../../components/ui/popups';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import { almTagsList } from '../../utils';

interface Props {
  component: Component;
  onComponentChange: (changes: {}) => void;
}

export default function MetaTags(props: Props) {
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(false);
  }, [props.component.tags]);

  const canUpdateTags = () => {
    const { configuration } = props.component;
    return configuration?.showSettings;
  };

  const setTags = (values: string[]) => {
    const { component } = props;

    if (component.qualifier === ComponentQualifier.Application) {
      return setApplicationTags({
        application: component.key,
        tags: values.join(','),
      });
    }

    return setProjectTags({
      project: component.key,
      tags: values.join(','),
    });
  };

  const handleSetProjectTags = (values: string[]) => {
    setLoading(true);
    setTags(values).then(
      () => props.onComponentChange({ tags: values }),
      () => {},
    );
  };

  const tags = props.component.tags ?? [];

  return (
    <>
      <Heading as="h3" className="sw-mb-2">
        {translate('tags')}
      </Heading>
      <Tags
        allowUpdate={canUpdateTags()}
        ariaTagsListLabel={translate('tags')}
        className="project-info-tags"
        emptyText={translate('no_tags')}
        overlay={
          <Spinner isLoading={loading}>
            <MetaTagsSelector selectedTags={tags} setProjectTags={handleSetProjectTags} />
          </Spinner>
        }
        popupPlacement={PopupPlacement.Bottom}
        tags={tags}
        tagsToDisplay={2}
        tooltip={Tooltip}
      />
    </>
  );
}

interface MetaTagsSelectorProps {
  selectedTags: string[];
  setProjectTags: (tags: string[]) => void;
}

const LIST_SIZE = 10;
const MAX_LIST_SIZE = 100;

function MetaTagsSelector({ selectedTags, setProjectTags }: MetaTagsSelectorProps) {
  const [searchResult, setSearchResult] = useState<string[]>([]);
  const availableTags = difference(searchResult, selectedTags);

  const onSearch = (query: string) => {
    return searchProjectTags({
      q: query,
      ps: Math.min(selectedTags.length - 1 + LIST_SIZE, MAX_LIST_SIZE),
    }).then(
      ({ tags }) => setSearchResult(tags.filter((item) => !almTagsList.includes(item))),
      () => {},
    );
  };

  const onSelect = (tag: string) => {
    if (!almTagsList.find((t) => t === tag)) {
      setProjectTags([...selectedTags, tag]);
    } else {
      addGlobalErrorMessage('Integration type tag cannot be added to the project.');
    }
  };

  const onUnselect = (tag: string) => {
    if (!almTagsList.find((t) => t === tag)) {
      setProjectTags(without(selectedTags, tag));
    } else {
      addGlobalErrorMessage('Integration type tags cannot be removed from projects.');
    }
  };

  return (
    <MultiSelector
      headerLabel={translate('tags')}
      searchInputAriaLabel={translate('search.search_for_tags')}
      createElementLabel={translate('issue.create_tag')}
      noResultsLabel={translate('no_results')}
      onSearch={onSearch}
      onSelect={onSelect}
      onUnselect={onUnselect}
      selectedElements={selectedTags}
      elements={availableTags.filter((item) => !almTagsList.includes(item))}
    />
  );
}
