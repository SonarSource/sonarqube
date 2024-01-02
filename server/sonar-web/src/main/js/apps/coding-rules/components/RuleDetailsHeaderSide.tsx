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
import { LightLabel, themeBorder } from 'design-system';
import React from 'react';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPillList from '../../../components/shared/SoftwareImpactPillList';
import { translate } from '../../../helpers/l10n';
import { RuleDetails } from '../../../types/types';

interface Props {
  ruleDetails: RuleDetails;
}

export default function RuleDetailsHeaderSide({ ruleDetails }: Readonly<Props>) {
  return (
    <StyledSection className="sw-flex sw-flex-col sw-pl-4 sw-gap-6 sw-max-w-[250px]">
      {ruleDetails.cleanCodeAttributeCategory && ruleDetails.cleanCodeAttribute && (
        <RuleHeaderInfo title={translate('coding_rules.cct_attribute.label')}>
          <CleanCodeAttributePill
            cleanCodeAttributeCategory={ruleDetails.cleanCodeAttributeCategory}
            cleanCodeAttribute={ruleDetails.cleanCodeAttribute}
            type="rule"
          />
        </RuleHeaderInfo>
      )}

      <RuleHeaderInfo title={translate('coding_rules.software_qualities.label')}>
        <SoftwareImpactPillList
          className="sw-flex-wrap"
          softwareImpacts={ruleDetails.impacts}
          type="rule"
        />
      </RuleHeaderInfo>
    </StyledSection>
  );
}

interface RuleHeaderMetaItemProps {
  children: React.ReactNode;
  title: string;
  className?: string;
}

function RuleHeaderInfo({ children, title, ...props }: Readonly<RuleHeaderMetaItemProps>) {
  return (
    <div {...props}>
      <LightLabel as="div" className="sw-text-xs sw-font-semibold sw-mb-1">
        {title}
      </LightLabel>
      {children}
    </div>
  );
}

const StyledSection = styled.div`
  border-left: ${themeBorder('default', 'pageBlockBorder')};
`;
