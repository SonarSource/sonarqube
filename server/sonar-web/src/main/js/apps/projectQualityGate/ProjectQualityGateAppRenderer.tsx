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

import { Link, LinkHighlight } from '@sonarsource/echoes-react';
import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { OptionProps, SingleValueProps, components } from 'react-select';
import {
  ButtonPrimary,
  FlagMessage,
  HelperHintIcon,
  InputSelect,
  LargeCenteredLayout,
  LightLabel,
  PageContentFontWrapper,
  RadioButton,
  Spinner,
  Title,
} from '~design-system';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { AiCodeAssuranceStatus } from '../../api/ai-code-assurance';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../app/components/available-features/withAvailableFeatures';
import DisableableSelectOption from '../../components/common/DisableableSelectOption';
import DocumentationLink from '../../components/common/DocumentationLink';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import AIAssuredIcon, { AiIconColor } from '../../components/icon-mappers/AIAssuredIcon';
import { AiIconVariant } from '../../components/illustrations/AiAssuredIllustration';
import AiCodeAssuranceBanner from '../../components/ui/AiCodeAssuranceBanner';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { LabelValueSelectOption } from '../../helpers/search';
import { getQualityGateUrl } from '../../helpers/urls';
import { useProjectAiCodeAssuranceStatusQuery } from '../../queries/ai-code-assurance';
import {
  useAssociateGateWithProjectMutation,
  useDissociateGateWithProjectMutation,
} from '../../queries/quality-gates';
import { ComponentQualifier } from '../../sonar-aligned/types/component';
import { Feature } from '../../types/features';
import { Component, Organization, QualityGate } from '../../types/types';
import BuiltInQualityGateBadge from '../quality-gates/components/BuiltInQualityGateBadge';
import AiAssuranceSuccessMessage from './AiAssuranceSuccessMessage';
import AiAssuranceWarningMessage from './AiAssuranceWarningMessage';
import { USE_SYSTEM_DEFAULT } from './constants';

export interface ProjectQualityGateAppRendererProps extends WithAvailableFeaturesProps {
  organization: Organization;
  allQualityGates?: QualityGate[];
  component: Component;
  currentQualityGate?: QualityGate;
  loading: boolean;
  onSelect: (id: string) => void;
  onSubmit: () => void;
  selectedQualityGateName: string;
}

function hasConditionOnNewCode(qualityGate: QualityGate): boolean {
  return !!qualityGate.conditions?.some((condition) => isDiffMetric(condition.metric));
}

interface QualityGateOption extends LabelValueSelectOption {
  organizationKey: string;
  isAiAssured: boolean;
  isDisabled: boolean;
}

function renderOption(data: QualityGateOption) {
  return (
    <div className="sw-flex sw-items-center sw-justify-between">
      <DisableableSelectOption
        className="sw-mr-2"
        option={data}
        disabledReason={translate('project_quality_gate.no_condition.reason')}
        disableTooltipOverlay={() => (
          <FormattedMessage
            id="project_quality_gate.no_condition"
            defaultMessage={translate('project_quality_gate.no_condition')}
            values={{
              link: (
                <Link to={getQualityGateUrl(data.organizationKey, data.label)}>
                  {translate('project_quality_gate.no_condition.link')}
                </Link>
              ),
            }}
          />
        )}
      />
      {data.isAiAssured && (
        <AIAssuredIcon
          variant={AiIconVariant.Default}
          color={AiIconColor.Subdued}
          width={16}
          height={16}
        />
      )}
    </div>
  );
}

function renderQualityGateOption(props: OptionProps<QualityGateOption, false>) {
  return <components.Option {...props}>{renderOption(props.data)}</components.Option>;
}

function singleValueRenderer(props: SingleValueProps<QualityGateOption, false>) {
  return <components.SingleValue {...props}>{renderOption(props.data)}</components.SingleValue>;
}

function ProjectQualityGateAppRenderer(props: Readonly<ProjectQualityGateAppRendererProps>) {
  const { organization, allQualityGates, component, currentQualityGate, loading, selectedQualityGateName } =
    props;
  const defaultQualityGate = allQualityGates?.find((g) => g.isDefault);
  const [isUserEditing, setIsUserEditing] = useState(false);

  const { data: aiAssuranceStatus, refetch: refetchAiCodeAssuranceStatus } =
    useProjectAiCodeAssuranceStatusQuery(
      { project: component.key },
      {
        enabled:
          component.qualifier === ComponentQualifier.Project &&
          props.hasFeature(Feature.AiCodeAssurance),
      },
    );

  const { mutateAsync: associateGateWithProject, isPending: associateIsPending } =
    useAssociateGateWithProjectMutation();
  const { mutateAsync: dissociateGateWithProject, isPending: dissociateisPending } =
    useDissociateGateWithProjectMutation();
  const submitting = associateIsPending || dissociateisPending;

  const handleSubmit = async () => {
    const { allQualityGates, currentQualityGate, selectedQualityGateName } = props;

    if (allQualityGates === undefined || currentQualityGate === undefined) {
      return;
    }

    if (selectedQualityGateName === USE_SYSTEM_DEFAULT) {
      await dissociateGateWithProject({
        projectKey: component.key,
        organization: organization.kee,
      });
    } else {
      await associateGateWithProject({
        gateName: selectedQualityGateName,
        projectKey: component.key,
        organization: organization.kee,
      });
    }

    props.onSubmit();
  };

  if (loading) {
    return <Spinner />;
  }

  if (
    allQualityGates === undefined ||
    defaultQualityGate === undefined ||
    currentQualityGate === undefined
  ) {
    return null;
  }

  const usesDefault = selectedQualityGateName === USE_SYSTEM_DEFAULT;
  const needsReanalysis = usesDefault
    ? // currentQualityGate.isDefault is not always up to date. We need to check
      // against defaultQualityGate explicitly.
      defaultQualityGate.name !== currentQualityGate.name
    : selectedQualityGateName !== currentQualityGate.name;

  const selectedQualityGate = allQualityGates.find((qg) => qg.name === selectedQualityGateName);

  const options: QualityGateOption[] = allQualityGates.map((g) => ({
    organizationKey: organization.kee,
    isDisabled: g.conditions === undefined || g.conditions.length === 0,
    isAiAssured: g.isAiCodeSupported ?? false,
    label: g.name,
    value: g.name,
  }));

  const containsAiCode =
    aiAssuranceStatus === AiCodeAssuranceStatus.AI_CODE_ASSURED ||
    aiAssuranceStatus === AiCodeAssuranceStatus.CONTAINS_AI_CODE;

  return (
    <LargeCenteredLayout id="project-quality-gate">
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <Suggestions suggestion={DocLink.CaYC} />
        <Helmet defer={false} title={translate('project_quality_gate.page')} />
        <A11ySkipTarget anchor="qg_main" />

        <header className="sw-mb-5 sw-flex sw-items-center">
          <Helmet defer={false} title={translate('project_quality_gate.page')} />
          <Title>{translate('project_quality_gate.page')}</Title>
          <HelpTooltip
            className="sw-ml-2 sw-mb-4"
            overlay={translate('quality_gates.projects.help')}
          >
            <HelperHintIcon />
          </HelpTooltip>
        </header>

        <div className="sw-flex sw-flex-col sw-items-start">
          {aiAssuranceStatus === AiCodeAssuranceStatus.AI_CODE_ASSURED && (
            <AiCodeAssuranceBanner
              className="sw-mb-10 sw-w-abs-800"
              iconVariant={AiIconVariant.Check}
              title={
                <FormattedMessage id="project_quality_gate.ai_generated_code_protected.title" />
              }
              description={
                <FormattedMessage
                  id="project_quality_gate.ai_generated_code_protected.description"
                  values={{
                    p: (text) => <p>{text}</p>,
                    link: (text) => (
                      <DocumentationLink
                        highlight={LinkHighlight.Default}
                        className="sw-inline-block"
                        shouldOpenInNewTab
                        to={DocLink.AiCodeAssurance}
                      >
                        {text}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
            />
          )}

          {aiAssuranceStatus === AiCodeAssuranceStatus.CONTAINS_AI_CODE && (
            <AiCodeAssuranceBanner
              className="sw-mb-10 sw-w-abs-800"
              iconVariant={AiIconVariant.Default}
              title={
                <FormattedMessage id="project_quality_gate.ai_generated_code_not_protected.title" />
              }
              description={
                <FormattedMessage
                  id="project_quality_gate.ai_generated_code_not_protected.description"
                  values={{
                    p: (text) => <p>{text}</p>,
                    link: (text) => (
                      <DocumentationLink
                        highlight={LinkHighlight.Default}
                        shouldOpenInNewTab
                        to={DocLink.AiCodeAssurance}
                      >
                        {text}
                      </DocumentationLink>
                    ),
                    linkSonarWay: (text) => (
                      <Link
                        highlight={LinkHighlight.Default}
                        to={{
                          pathname: '/quality_gates/show/Sonar%20way%20for%20AI%20Code',
                        }}
                      >
                        {text}
                      </Link>
                    ),
                    linkQualifyDoc: (text) => (
                      <DocumentationLink
                        highlight={LinkHighlight.Default}
                        shouldOpenInNewTab
                        to={DocLink.AiCodeAssuranceQualifyQualityGate}
                      >
                        {text}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
            />
          )}

          <form
            onSubmit={async (e) => {
              e.preventDefault();
              await handleSubmit();
              setIsUserEditing(false);
//               refetchAiCodeAssuranceStatus();
            }}
            id="project_quality_gate"
          >
            <p className="sw-mb-4">{translate('project_quality_gate.page.description')}</p>

            <div className="sw-mb-4">
              <RadioButton
                className="it__project-quality-default sw-items-start"
                checked={usesDefault}
                disabled={submitting}
                onCheck={() => {
                  setIsUserEditing(true);
                  props.onSelect(USE_SYSTEM_DEFAULT);
                }}
                value={USE_SYSTEM_DEFAULT}
              >
                <div>
                  <div className="sw-ml-1 sw-mb-2">
                    {translate('project_quality_gate.always_use_default')}
                  </div>
                  <div>
                    <LightLabel>
                      {translate('current_noun')}: {defaultQualityGate.name}
                      {defaultQualityGate.isAiCodeSupported && (
                        <AIAssuredIcon
                          className="sw-ml-1"
                          variant={AiIconVariant.Default}
                          color={AiIconColor.Subdued}
                          width={16}
                          height={16}
                        />
                      )}
                      {defaultQualityGate.isBuiltIn && (
                        <BuiltInQualityGateBadge className="sw-ml-1" />
                      )}
                    </LightLabel>
                  </div>
                  {containsAiCode &&
                    isUserEditing &&
                    usesDefault &&
                    defaultQualityGate.isAiCodeSupported === true && (
                      <AiAssuranceSuccessMessage className="sw-mt-1" />
                    )}

                  {containsAiCode &&
                    isUserEditing &&
                    usesDefault &&
                    defaultQualityGate.isAiCodeSupported === false && (
                      <AiAssuranceWarningMessage className="sw-mt-1" />
                    )}
                </div>
              </RadioButton>
            </div>

            <div className="sw-mb-4">
              <RadioButton
                className="it__project-quality-specific sw-items-start sw-mt-1"
                checked={!usesDefault}
                disabled={submitting}
                onCheck={(value: string) => {
                  setIsUserEditing(true);
                  if (usesDefault) {
                    props.onSelect(value);
                  }
                }}
                value={!usesDefault ? selectedQualityGateName : currentQualityGate.name}
              >
                <div>
                  <div className="sw-ml-1 sw-mb-2">
                    {translate('project_quality_gate.always_use_specific')}
                  </div>
                </div>
              </RadioButton>
              <div className="sw-ml-6">
                <InputSelect
                  size="large"
                  className="it__project-quality-gate-select"
                  components={{
                    Option: renderQualityGateOption,
                    SingleValue: singleValueRenderer,
                  }}
                  isClearable={usesDefault}
                  isDisabled={submitting || usesDefault}
                  onChange={({ value }: QualityGateOption) => {
                    setIsUserEditing(true);
                    props.onSelect(value);
                  }}
                  aria-label={translate('project_quality_gate.select_specific_qg')}
                  options={options}
                  value={options.find((o) => o.value === selectedQualityGateName)}
                />
              </div>
              {containsAiCode &&
                isUserEditing &&
                !usesDefault &&
                selectedQualityGate?.isAiCodeSupported === true && (
                  <AiAssuranceSuccessMessage className="sw-mt-1 sw-ml-6" />
                )}

              {containsAiCode &&
                isUserEditing &&
                !usesDefault &&
                selectedQualityGate &&
                selectedQualityGate.isAiCodeSupported === false && (
                  <AiAssuranceWarningMessage className="sw-mt-1 sw-ml-6" />
                )}

              {selectedQualityGate && !hasConditionOnNewCode(selectedQualityGate) && (
                <FlagMessage variant="warning">
                  <FormattedMessage
                    id="project_quality_gate.no_condition_on_new_code"
                    defaultMessage={translate('project_quality_gate.no_condition_on_new_code')}
                    values={{
                      link: (
                        <Link to={getQualityGateUrl(organization.kee, selectedQualityGate.name)}>
                          {translate('project_quality_gate.no_condition.link')}
                        </Link>
                      ),
                    }}
                  />
                </FlagMessage>
              )}
              {needsReanalysis && (
                <FlagMessage className="sw-mt-4 sw-w-abs-600" variant="warning">
                  {translate('project_quality_gate.requires_new_analysis')}
                </FlagMessage>
              )}
            </div>

            <div>
              <ButtonPrimary form="project_quality_gate" disabled={submitting} type="submit">
                {translate('save')}
              </ButtonPrimary>
              <Spinner loading={submitting} />
            </div>
          </form>
        </div>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withAvailableFeatures(ProjectQualityGateAppRenderer);
