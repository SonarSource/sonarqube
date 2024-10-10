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
  FlagMessage,
  PageContentFontWrapper,
  RadioButton,
  SelectionCard,
  themeColor,
} from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { getNewCodeDefinition } from '../../api/newCodeDefinition';
import { translate } from '../../helpers/l10n';
import {
  getNumberOfDaysDefaultValue,
  isNewCodeDefinitionCompliant,
} from '../../helpers/new-code-definition';
import {
  NewCodeDefinition,
  NewCodeDefinitionType,
  NewCodeDefinitiondWithCompliance,
} from '../../types/new-code-definition';
import GlobalNewCodeDefinitionDescription from './GlobalNewCodeDefinitionDescription';
import NewCodeDefinitionDaysOption from './NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from './NewCodeDefinitionPreviousVersionOption';
import { NewCodeDefinitionLevels } from './utils';

interface Props {
  isMultipleProjects?: boolean;
  onNcdChanged: (ncd: NewCodeDefinitiondWithCompliance) => void;
}

export default function NewCodeDefinitionSelector(props: Props) {
  const { onNcdChanged, isMultipleProjects } = props;

  const [globalNcd, setGlobalNcd] = React.useState<NewCodeDefinition | null>(null);
  const [selectedNcdType, setSelectedNcdType] = React.useState<NewCodeDefinitionType | null>(null);
  const [days, setDays] = React.useState<string>('');

  React.useEffect(() => {
    const numberOfDays = getNumberOfDaysDefaultValue(globalNcd);
    setDays(numberOfDays);
  }, [globalNcd]);

  const isCompliant = React.useMemo(
    () =>
      !!selectedNcdType &&
      isNewCodeDefinitionCompliant({
        type: selectedNcdType,
        value: days,
      }),
    [selectedNcdType, days],
  );

  const handleNcdChanged = React.useCallback(
    (newNcdType: NewCodeDefinitionType) => {
      if (newNcdType && newNcdType !== selectedNcdType) {
        setSelectedNcdType(newNcdType);
      }
    },
    [selectedNcdType],
  );

  React.useEffect(() => {
    function fetchGlobalNcd() {
      getNewCodeDefinition().then(setGlobalNcd, noop);
    }

    fetchGlobalNcd();
  }, []);

  React.useEffect(() => {
    if (selectedNcdType) {
      const type =
        selectedNcdType === NewCodeDefinitionType.Inherited ? undefined : selectedNcdType;
      const value = selectedNcdType === NewCodeDefinitionType.NumberOfDays ? days : undefined;
      onNcdChanged({ isCompliant, type, value });
    }
  }, [selectedNcdType, days, isCompliant, onNcdChanged]);

  return (
    <PageContentFontWrapper>
      <p className="sw-mt-10">
        <strong className="sw-typo-lg-semibold">
          {isMultipleProjects
            ? translate('new_code_definition.question.multiple_projects')
            : translate('new_code_definition.question')}
        </strong>
      </p>
      <div className="sw-mt-7 sw-ml-1" role="radiogroup">
        <RadioButton
          aria-label={translate('new_code_definition.global_setting')}
          checked={selectedNcdType === NewCodeDefinitionType.Inherited}
          onCheck={() => handleNcdChanged(NewCodeDefinitionType.Inherited)}
          value="general"
        >
          <span className="sw-font-semibold">
            {translate('new_code_definition.global_setting')}
          </span>
        </RadioButton>

        <StyledGlobalSettingWrapper
          className="sw-mt-4 sw-ml-6"
          selected={selectedNcdType === NewCodeDefinitionType.Inherited}
        >
          {globalNcd && <GlobalNewCodeDefinitionDescription globalNcd={globalNcd} />}
        </StyledGlobalSettingWrapper>

        <RadioButton
          aria-label={
            isMultipleProjects
              ? translate('new_code_definition.specific_setting.multiple_projects')
              : translate('new_code_definition.specific_setting')
          }
          checked={Boolean(selectedNcdType && selectedNcdType !== NewCodeDefinitionType.Inherited)}
          className="sw-mt-12 sw-font-semibold"
          onCheck={() => handleNcdChanged(NewCodeDefinitionType.PreviousVersion)}
          value="specific"
        >
          {isMultipleProjects
            ? translate('new_code_definition.specific_setting.multiple_projects')
            : translate('new_code_definition.specific_setting')}
        </RadioButton>
      </div>

      <div className="sw-flex sw-flex-col sw-my-4 sw-mr-4 sw-gap-4" role="radiogroup">
        <NewCodeDefinitionPreviousVersionOption
          disabled={Boolean(
            !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited,
          )}
          onSelect={handleNcdChanged}
          selected={selectedNcdType === NewCodeDefinitionType.PreviousVersion}
        />

        <NewCodeDefinitionDaysOption
          days={days}
          disabled={Boolean(
            !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited,
          )}
          isValid={isCompliant}
          onChangeDays={setDays}
          onSelect={handleNcdChanged}
          selected={selectedNcdType === NewCodeDefinitionType.NumberOfDays}
          settingLevel={NewCodeDefinitionLevels.NewProject}
        />

        <SelectionCard
          disabled={Boolean(
            !selectedNcdType || selectedNcdType === NewCodeDefinitionType.Inherited,
          )}
          onClick={() => handleNcdChanged(NewCodeDefinitionType.ReferenceBranch)}
          selected={selectedNcdType === NewCodeDefinitionType.ReferenceBranch}
          title={translate('new_code_definition.reference_branch')}
        >
          <div>
            <p className="sw-mb-2">
              {translate('new_code_definition.reference_branch.description')}
            </p>
            <p>{translate('new_code_definition.reference_branch.usecase')}</p>
            {selectedNcdType === NewCodeDefinitionType.ReferenceBranch && (
              <FlagMessage className="sw-mt-4" variant="info">
                {translate('new_code_definition.reference_branch.notice')}
              </FlagMessage>
            )}
          </div>
        </SelectionCard>
      </div>
    </PageContentFontWrapper>
  );
}

const StyledGlobalSettingWrapper = styled.div<{ selected: boolean }>`
  color: ${({ selected }) => (selected ? 'inherit' : themeColor('selectionCardDisabledText'))};
`;
