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
  LinkStandalone,
  Spinner,
  Text
} from '@sonarsource/echoes-react';
import { MutationStatus } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import React, { useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import { BasicSeparator, HighlightedSection, themeColor, UnorderedList } from '~design-system';
import { SuggestionServiceStatusCheckResponse } from '../../../api/fix-suggestions';
import withAvailableFeatures, {
  WithAvailableFeaturesProps
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import { getAiCodeFixTermsOfServiceUrl } from '../../../helpers/urls';
import { useCheckServiceMutation, useRemoveCodeSuggestionsCache } from '../../../queries/fix-suggestions';
import { useGetValueQuery, useSaveSimpleValueMutation } from '../../../queries/settings';
import { Feature } from '../../../types/features';
import { SettingsKey } from '../../../types/settings';
import PromotedSection from '../../overview/branches/PromotedSection';

interface Props extends WithAvailableFeaturesProps {}

const CODE_FIX_SETTING_KEY = SettingsKey.CodeSuggestion;

function CodeFixAdmin({ hasFeature }: Readonly<Props>) {
  const { data: codeFixSetting } = useGetValueQuery({
    key: CODE_FIX_SETTING_KEY,
  });

  const removeCodeSuggestionsCache = useRemoveCodeSuggestionsCache();

  const { mutate: saveSetting } = useSaveSimpleValueMutation();

  const isCodeFixEnabled = codeFixSetting?.value === 'true';

  const [enableCodeFix, setEnableCodeFix] = React.useState(isCodeFixEnabled);
  const [acceptedTerms, setAcceptedTerms] = React.useState(false);
  const {
    mutate: checkService,
    isIdle,
    isPending,
    status,
    error,
    data,
  } = useCheckServiceMutation();
  const isValueChanged = enableCodeFix !== isCodeFixEnabled;

  useEffect(() => {
    setEnableCodeFix(isCodeFixEnabled);
  }, [isCodeFixEnabled]);

  const handleSave = () => {
    saveSetting(
      { key: CODE_FIX_SETTING_KEY, value: enableCodeFix ? 'true' : 'false' },
      {
        onSuccess: removeCodeSuggestionsCache,
      },
    );
  };

  const handleCancel = () => {
    setEnableCodeFix(isCodeFixEnabled);
    setAcceptedTerms(false);
  };

  if (!hasFeature(Feature.FixSuggestions)) {
    return null;
  }

  return (
    <div className="sw-flex">
      <div className="sw-flex-grow sw-p-6">
        <Heading as="h2" hasMarginBottom>
          {translate('property.codefix.admin.title')}
        </Heading>
        <PromotedSection
          content={
            <>
              <p>{translate('property.codefix.admin.promoted_section.content1')}</p>
              <p className="sw-mt-2">
                {translate('property.codefix.admin.promoted_section.content2')}
              </p>
            </>
          }
          title={translate('property.codefix.admin.promoted_section.title')}
        />
        <p>{translate('property.codefix.admin.description')}</p>
        <Checkbox
          className="sw-mt-6"
          label={translate('property.codefix.admin.checkbox.label')}
          checked={Boolean(enableCodeFix)}
          onCheck={() => setEnableCodeFix(!enableCodeFix)}
        />
        {isValueChanged && (
          <div>
            <BasicSeparator className="sw-mt-6" />
            {enableCodeFix && (
              <Checkbox
                className="sw-mt-6"
                label={
                  <FormattedMessage
                    id="property.codefix.admin.terms"
                    defaultMessage={translate('property.codefix.admin.acceptTerm.label')}
                    values={{
                      terms: (
                        <LinkStandalone to={getAiCodeFixTermsOfServiceUrl()}>
                          {translate('property.codefix.admin.acceptTerm.terms')}
                        </LinkStandalone>
                      ),
                    }}
                  />
                }
                checked={acceptedTerms}
                onCheck={() => setAcceptedTerms(!acceptedTerms)}
              />
            )}
            <div className="sw-mt-6">
              <Button
                variety={ButtonVariety.Primary}
                isDisabled={!acceptedTerms && enableCodeFix}
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
        )}
      </div>
      <div className="sw-flex-col sw-w-abs-600 sw-p-6">
        <HighlightedSection className="sw-items-start">
          <Heading as="h3" hasMarginBottom>
            {translate('property.codefix.admin.serviceCheck.title')}
          </Heading>
          <p>{translate('property.codefix.admin.serviceCheck.description1')}</p>
          <p className="sw-mt-4">{translate('property.codefix.admin.serviceCheck.description2')}</p>
          <Button
            className="sw-mt-4"
            variety={ButtonVariety.Default}
            onClick={() => checkService()}
            isDisabled={isPending}
          >
            {translate('property.codefix.admin.serviceCheck.action')}
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

interface ServiceCheckResultViewProps {
  data: SuggestionServiceStatusCheckResponse | undefined;
  error: AxiosError | null;
  status: MutationStatus;
}

function ServiceCheckResultView({ data, error, status }: Readonly<ServiceCheckResultViewProps>) {
  switch (status) {
    case 'pending':
      return <Spinner label={translate('property.codefix.admin.serviceCheck.spinner.label')} />;
    case 'error':
      return (
        <ErrorMessage
          text={`${translate('property.codefix.admin.serviceCheck.result.requestError')} ${error?.status ?? 'No status'}`}
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
        <SuccessMessage text={translate('property.codefix.admin.serviceCheck.result.success')} />
      );
    case 'TIMEOUT':
    case 'CONNECTION_ERROR':
      return (
        <div className="sw-flex">
          <IconError className="sw-mr-1" color="echoes-color-icon-danger" />
          <div className="sw-flex-col">
            <ErrorLabel
              text={translate('property.codefix.admin.serviceCheck.result.unresponsive.message')}
            />
            <p className="sw-mt-4">
              <ErrorLabel
                text={translate(
                  'property.codefix.admin.serviceCheck.result.unresponsive.causes.title',
                )}
              />
            </p>
            <UnorderedList className="sw-ml-8" ticks>
              <ErrorListItem className="sw-mb-2">
                <ErrorLabel
                  text={translate(
                    'property.codefix.admin.serviceCheck.result.unresponsive.causes.1',
                  )}
                />
              </ErrorListItem>
              <ErrorListItem>
                <ErrorLabel
                  text={translate(
                    'property.codefix.admin.serviceCheck.result.unresponsive.causes.2',
                  )}
                />
              </ErrorListItem>
            </UnorderedList>
          </div>
        </div>
      );
    case 'UNAUTHORIZED':
      return (
        <ErrorMessage text={translate('property.codefix.admin.serviceCheck.result.unauthorized')} />
      );
    case 'SERVICE_ERROR':
      return (
        <ErrorMessage text={translate('property.codefix.admin.serviceCheck.result.serviceError')} />
      );
    default:
      return (
        <ErrorMessage
          text={`${translate('property.codefix.admin.serviceCheck.result.unknown')} ${data?.status ?? 'no status returned from the service'}`}
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

export default withAvailableFeatures(CodeFixAdmin);
