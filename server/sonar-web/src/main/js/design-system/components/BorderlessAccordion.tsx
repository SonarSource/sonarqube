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
import { uniqueId } from 'lodash';
import * as React from 'react';
import { BareButton } from '../sonar-aligned/components/buttons';
import { OpenCloseIndicator } from './icons/OpenCloseIndicator';

interface AccordionProps {
  ariaLabel?: string;
  children: React.ReactNode;
  className?: string;
  data?: string;
  header: React.ReactNode;
  onClick: (data?: string) => void;
  open: boolean;
}

export function BorderlessAccordion(props: AccordionProps) {
  const { ariaLabel, className, open, header, data, onClick } = props;

  const id = React.useMemo(() => uniqueId('accordion-'), []);
  const handleClick = React.useCallback(() => {
    onClick(data);
  }, [onClick, data]);

  return (
    <div
      className={classNames('sw-cursor-pointer', className, {
        open,
      })}
      role="listitem"
    >
      <BareButton
        aria-controls={`${id}-panel`}
        aria-expanded={open}
        aria-label={ariaLabel}
        className="sw-flex sw-items-center sw-justify-between sw-px-2 sw-py-2 sw-box-border sw-w-full"
        id={`${id}-header`}
        onClick={handleClick}
      >
        {header}
        <OpenCloseIndicator aria-hidden className="sw-ml-2" open={open} />
      </BareButton>
      <div aria-labelledby={`${id}-header`} id={`${id}-panel`} role="region">
        {open && <div>{props.children}</div>}
      </div>
    </div>
  );
}
