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
  DismissableFlagMessage,
  FlagErrorIcon,
  InputField,
  Note,
  SelectionCard,
} from 'design-system';
import { noop } from 'lodash';
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { MessageTypes, checkMessageDismissed, setMessageDismissed } from '../../api/messages';
import { DocLink } from '../../helpers/doc-links';
import { translate, translateWithParameters } from '../../helpers/l10n';
import {
  NUMBER_OF_DAYS_MAX_VALUE,
  NUMBER_OF_DAYS_MIN_VALUE,
} from '../../helpers/new-code-definition';
import { isDefined } from '../../helpers/types';
import { NewCodeDefinitionType } from '../../types/new-code-definition';
import DocumentationLink from '../common/DocumentationLink';
import { NewCodeDefinitionLevels } from './utils';

export interface Props {
  className?: string;
  currentDaysValue?: string;
  days: string;
  disabled?: boolean;
  isChanged: boolean;
  isValid: boolean;
  onChangeDays: (value: string) => void;
  onSelect: (selection: NewCodeDefinitionType) => void;
  previousNonCompliantValue?: string;
  projectKey?: string;
  selected: boolean;
  settingLevel: NewCodeDefinitionLevels;
  updatedAt?: number;
}

export default function NewCodeDefinitionDaysOption(props: Props) {
  const {
    className,
    days,
    currentDaysValue,
    previousNonCompliantValue,
    projectKey,
    updatedAt,
    disabled,
    isChanged,
    isValid,
    onChangeDays,
    onSelect,
    selected,
    settingLevel,
  } = props;

  const [ncdAutoUpdateBannerDismissed, setNcdAutoUpdateBannerDismissed] = useState(true);

  useEffect(() => {
    async function fetchMessageDismissed() {
      const messageStatus = await checkMessageDismissed(
        projectKey
          ? {
              messageType: MessageTypes.ProjectNcdPage90,
              projectKey,
            }
          : {
              messageType: MessageTypes.GlobalNcdPage90,
            },
      );
      setNcdAutoUpdateBannerDismissed(messageStatus.dismissed);
    }

    if (isDefined(previousNonCompliantValue)) {
      fetchMessageDismissed().catch(noop);
    }
  }, [previousNonCompliantValue, projectKey, settingLevel]);

  const shouldShowAutoUpdateBanner = useMemo(() => {
    return (
      (settingLevel === NewCodeDefinitionLevels.Global ||
        settingLevel === NewCodeDefinitionLevels.Project) &&
      isDefined(previousNonCompliantValue) &&
      isDefined(updatedAt) &&
      !disabled &&
      !ncdAutoUpdateBannerDismissed
    );
  }, [disabled, ncdAutoUpdateBannerDismissed, previousNonCompliantValue, settingLevel, updatedAt]);

  const handleBannerDismiss = useCallback(async () => {
    await setMessageDismissed({ messageType: MessageTypes.GlobalNcdPage90 });
    setNcdAutoUpdateBannerDismissed(true);
  }, []);

  return (
    <SelectionCard
      className={className}
      disabled={disabled}
      onClick={() => onSelect(NewCodeDefinitionType.NumberOfDays)}
      selected={selected}
      title={translate('new_code_definition.number_days')}
    >
      <>
        <div>
          <p className="sw-mb-2">{translate('new_code_definition.number_days.description')}</p>
          <p>{translate('new_code_definition.number_days.usecase')}</p>
        </div>
        {selected && (
          <div className="sw-mt-4">
            <label>
              {translate('new_code_definition.number_days.specify_days')}
              <div className="sw-my-2 sw-flex sw-items-center">
                <InputField
                  id="baseline_number_of_days"
                  isInvalid={!isValid}
                  isValid={isChanged && isValid}
                  max={NUMBER_OF_DAYS_MAX_VALUE}
                  min={NUMBER_OF_DAYS_MIN_VALUE}
                  onChange={(e) => onChangeDays(e.currentTarget.value)}
                  required
                  type="number"
                  value={days}
                />
                {!isValid && <FlagErrorIcon className="sw-ml-2" />}
              </div>
            </label>
            <Note>
              {translateWithParameters(
                'new_code_definition.number_days.invalid',
                NUMBER_OF_DAYS_MIN_VALUE,
                NUMBER_OF_DAYS_MAX_VALUE,
              )}
            </Note>

            {shouldShowAutoUpdateBanner && (
              <DismissableFlagMessage
                variant="info"
                className="sw-mt-4 sw-max-w-[800px]"
                onDismiss={handleBannerDismiss}
              >
                <FormattedMessage
                  defaultMessage="new_code_definition.auto_update.ncd_page.message"
                  id="new_code_definition.auto_update.ncd_page.message"
                  tagName="span"
                  values={{
                    previousDays: previousNonCompliantValue,
                    days: currentDaysValue,
                    date: isDefined(updatedAt) && new Date(updatedAt).toLocaleDateString(),
                    link: (
                      <DocumentationLink to={DocLink.NewCodeDefinitionOptions}>
                        {translate('learn_more')}
                      </DocumentationLink>
                    ),
                  }}
                />
              </DismissableFlagMessage>
            )}
          </div>
        )}
      </>
    </SelectionCard>
  );
}
