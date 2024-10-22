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
import classNames from 'classnames';
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { BareButton } from '../sonar-aligned/components/buttons';
import { OpenCloseIndicator } from './icons/OpenCloseIndicator';

interface Props {
  children: ReactNode;
  expanded?: boolean;
  header: ReactNode;
  hidden?: boolean;
  id: string;
  innerRef?: (node: HTMLDivElement) => void;
  onClick?: () => void;
}

export function ExecutionFlowAccordion(props: Readonly<Props>) {
  const { children, expanded, header, hidden, id, innerRef, onClick } = props;

  return (
    <Accordion className={classNames({ expanded, 'sw-hidden': hidden })} ref={innerRef}>
      <Expander
        aria-controls={`${id}-flow-accordion`}
        aria-expanded={expanded}
        aria-hidden={hidden}
        id={`${id}-flow-accordion-button`}
        onClick={onClick}
      >
        {header}
        <OpenCloseIndicator open={Boolean(expanded)} />
      </Expander>

      {expanded && <Body id={`${id}-flow-accordion-body`}>{children}</Body>}
    </Accordion>
  );
}

const Expander = styled(BareButton)`
  ${tw`sw-flex sw-items-center sw-justify-between`}
  ${tw`sw-box-border`}
  ${tw`sw-p-2`}
  ${tw`sw-w-full`}
  ${tw`sw-cursor-pointer`}

  color: ${themeContrast('subnavigationExecutionFlow')};
  background-color: ${themeColor('subnavigationExecutionFlow')};
`;

const Accordion = styled.div`
  ${tw`sw-flex sw-flex-col`}
  ${tw`sw-rounded-1/2`}

  border: ${themeBorder('default', 'subnavigationExecutionFlowBorder')};

  &:hover {
    border: ${themeBorder('default', 'subnavigationExecutionFlowActive')};
  }

  &.expanded {
    border: ${themeBorder('default', 'subnavigationExecutionFlowActive')};
    outline: ${themeBorder('focus', 'primary')};

    ${Expander} {
      border-bottom: ${themeBorder('default', 'subnavigationExecutionFlowBorder')};
    }
  }
`;

const Body = styled.div`
  ${tw`sw-p-2`}

  background-color: ${themeColor('subnavigationExecutionFlow')};
`;

ExecutionFlowAccordion.displayName = 'ExecutionFlowAccordion';
