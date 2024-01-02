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
import { uniqueId } from 'lodash';
import React, { ReactNode } from 'react';
import tw from 'twin.macro';
import { themeColor } from '../helpers';
import { Note } from './Text';
import { BareButton } from './buttons';
import { OpenCloseIndicator } from './icons';

interface Props {
  ariaLabel: string;
  children: ReactNode;
  className?: string;
  data?: string;
  onClick: (data?: string) => void;
  open: boolean;
  renderHeader?: () => ReactNode;
  title: ReactNode;
}

export function TextAccordion(props: Readonly<Props>) {
  const [hoveringInner, setHoveringInner] = React.useState(false);

  const { className, open, renderHeader, title, ariaLabel } = props;

  const id = React.useMemo(() => uniqueId('accordion-'), []);

  function handleClick() {
    props.onClick(props.data);
  }

  function onDetailEnter() {
    setHoveringInner(true);
  }

  function onDetailLeave() {
    setHoveringInner(false);
  }

  return (
    <StyledAccordion
      className={classNames('it__text-accordion', className, {
        'no-hover': hoveringInner,
      })}
    >
      <Note as="h3">
        <BareButton
          aria-controls={`${id}-panel`}
          aria-expanded={open}
          aria-label={ariaLabel}
          className="sw-flex sw-items-center sw-px-2 sw-py-2 sw-box-border sw-w-full"
          id={`${id}-header`}
          onClick={handleClick}
        >
          <AccordionTitle>
            <OpenCloseIndicator className="sw-mr-1" open={open} />
            {title}
          </AccordionTitle>
          {renderHeader?.()}
        </BareButton>
      </Note>
      {open && (
        <AccordionContent onMouseEnter={onDetailEnter} onMouseLeave={onDetailLeave} role="region">
          {props.children}
        </AccordionContent>
      )}
    </StyledAccordion>
  );
}

const StyledAccordion = styled.div`
  transition: border-color 0.3s ease;
`;

const AccordionTitle = styled.span`
  cursor: pointer;
  position: relative;
  display: inline-flex;
  align-items: center;
  font-weight: bold;
  vertical-align: middle;
  transition: color 0.3s ease;

  ${tw`sw-select-none`}
  ${tw`sw-pt-4 sw-px-page sw-pb-2`}

  &:hover {
    color: ${themeColor('linkDefault')};
  }
`;

const AccordionContent = styled.div`
  ${tw`sw-pl-10`}
`;
