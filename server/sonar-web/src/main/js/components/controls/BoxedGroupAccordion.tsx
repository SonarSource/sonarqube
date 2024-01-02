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
import OpenCloseIcon from '../icons/OpenCloseIcon';
import { ButtonPlain } from './buttons';

interface BoxedGroupAccordionProps {
  children: React.ReactNode;
  noBorder?: boolean;
  className?: string;
  data?: string;
  onClick: (data?: string) => void;
  open: boolean;
  renderHeader?: () => React.ReactNode;
  title: React.ReactNode;
}

export default function BoxedGroupAccordion(props: BoxedGroupAccordionProps) {
  const { className, noBorder, open, renderHeader, title, data, onClick } = props;

  const id = React.useMemo(() => uniqueId('accordion-'), []);
  const handleClick = React.useCallback(() => {
    onClick(data);
  }, [onClick, data]);

  return (
    <div
      className={classNames('boxed-group boxed-group-accordion', className, {
        'no-border': noBorder,
        open,
      })}
      role="listitem"
    >
      {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
      <div onClick={handleClick} className="display-flex-center boxed-group-header">
        <ButtonPlain
          stopPropagation
          className="boxed-group-accordion-title flex-grow"
          onClick={handleClick}
          id={`${id}-header`}
          aria-controls={`${id}-panel`}
          aria-expanded={open}
        >
          {title}
        </ButtonPlain>
        {renderHeader && renderHeader()}
        <OpenCloseIcon aria-hidden className="spacer-left" open={open} />
      </div>
      <div id={`${id}-panel`} aria-labelledby={`${id}-header`} role="region">
        {open && <div className="boxed-group-inner">{props.children}</div>}
      </div>
    </div>
  );
}
