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
import classNames from 'classnames';
import { Spinner } from 'design-system';
import React, { useCallback, useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import NewCodeDefinitionDaysOption from '../../../components/new-code-definition/NewCodeDefinitionDaysOption';
import NewCodeDefinitionPreviousVersionOption from '../../../components/new-code-definition/NewCodeDefinitionPreviousVersionOption';
import { NewCodeDefinitionLevels } from '../../../components/new-code-definition/utils';
import { translate } from '../../../helpers/l10n';
import {
  getNumberOfDaysDefaultValue,
  isNewCodeDefinitionCompliant,
} from '../../../helpers/new-code-definition';
import {
  useNewCodeDefinitionMutation,
  useNewCodeDefinitionQuery,
} from '../../../queries/newCodeDefinition';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';

export default function NewCodeDefinition() {
  const [numberOfDays, setNumberOfDays] = React.useState(getNumberOfDaysDefaultValue());
  const [selectedNewCodeDefinitionType, setSelectedNewCodeDefinitionType] = React.useState<
    NewCodeDefinitionType | undefined
  >(undefined);

  const { data: newCodeDefinition, isLoading } = useNewCodeDefinitionQuery();
  const { isPending: isSaving, mutate: postNewCodeDefinition } = useNewCodeDefinitionMutation();

  const resetNewCodeDefinition = useCallback(() => {
    setSelectedNewCodeDefinitionType(newCodeDefinition?.type);
    setNumberOfDays(getNumberOfDaysDefaultValue(newCodeDefinition));
  }, [newCodeDefinition]);

  const onSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    const type = selectedNewCodeDefinitionType;
    const value = type === NewCodeDefinitionType.NumberOfDays ? numberOfDays : undefined;

    postNewCodeDefinition({
      type,
      value,
    });
  };

  useEffect(() => {
    resetNewCodeDefinition();
  }, [resetNewCodeDefinition]);

  const isValid =
    selectedNewCodeDefinitionType !== NewCodeDefinitionType.NumberOfDays ||
    isNewCodeDefinitionCompliant({ type: NewCodeDefinitionType.NumberOfDays, value: numberOfDays });

  const isFormTouched =
    selectedNewCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
      ? numberOfDays !== newCodeDefinition?.value
      : selectedNewCodeDefinitionType !== newCodeDefinition?.type;

  return (
    <>
      <h2
        className="settings-sub-category-name settings-definition-name"
        title={translate('settings.new_code_period.title')}
      >
        {translate('settings.new_code_period.title')}
      </h2>

      <ul className="settings-sub-categories-list">
        <li>
          <ul className="settings-definitions-list">
            <li>
              <div className="settings-definition">
                <div className="settings-definition-left">
                  <div className="small">
                    <p className="sw-mb-2">{translate('settings.new_code_period.description0')}</p>
                    <p className="sw-mb-2">{translate('settings.new_code_period.description1')}</p>
                    <p className="sw-mb-2">{translate('settings.new_code_period.description2')}</p>

                    <p className="sw-mb-2">
                      <FormattedMessage
                        defaultMessage={translate('settings.new_code_period.description3')}
                        id="settings.new_code_period.description3"
                        values={{
                          link: (
                            <DocumentationLink to="/project-administration/defining-new-code/">
                              {translate('settings.new_code_period.description3.link')}
                            </DocumentationLink>
                          ),
                        }}
                      />
                    </p>

                    <p className="sw-mt-4">
                      <strong>{translate('settings.new_code_period.question')}</strong>
                    </p>
                  </div>
                </div>

                <div className="settings-definition-right">
                  <Spinner loading={isLoading}>
                    <form className="sw-flex sw-flex-col sw-items-stretch" onSubmit={onSubmit}>
                      <NewCodeDefinitionPreviousVersionOption
                        isDefault
                        onSelect={setSelectedNewCodeDefinitionType}
                        selected={
                          selectedNewCodeDefinitionType === NewCodeDefinitionType.PreviousVersion
                        }
                      />
                      <NewCodeDefinitionDaysOption
                        className="spacer-top sw-mb-4"
                        days={numberOfDays}
                        currentDaysValue={
                          newCodeDefinition?.type === NewCodeDefinitionType.NumberOfDays
                            ? newCodeDefinition?.value
                            : undefined
                        }
                        previousNonCompliantValue={newCodeDefinition?.previousNonCompliantValue}
                        projectKey={newCodeDefinition?.projectKey}
                        updatedAt={newCodeDefinition?.updatedAt}
                        isChanged={isFormTouched}
                        isValid={isValid}
                        onChangeDays={setNumberOfDays}
                        onSelect={setSelectedNewCodeDefinitionType}
                        selected={
                          selectedNewCodeDefinitionType === NewCodeDefinitionType.NumberOfDays
                        }
                        settingLevel={NewCodeDefinitionLevels.Global}
                      />
                      <div className="sw-mt-4">
                        <p
                          className={classNames('spacer-bottom', {
                            'sw-invisible': !isFormTouched,
                          })}
                        >
                          {translate('baseline.next_analysis_notice')}
                        </p>
                        <Spinner className="spacer-right" loading={isSaving} />
                        {!isSaving && (
                          <>
                            <SubmitButton disabled={!isFormTouched || !isValid}>
                              {translate('save')}
                            </SubmitButton>
                            <ResetButtonLink
                              className="spacer-left"
                              disabled={!isFormTouched}
                              onClick={resetNewCodeDefinition}
                            >
                              {translate('cancel')}
                            </ResetButtonLink>
                          </>
                        )}
                      </div>
                    </form>
                  </Spinner>
                </div>
              </div>
            </li>
          </ul>
        </li>
      </ul>
    </>
  );
}
