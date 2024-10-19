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
import { Label } from '@sonarsource/echoes-react';
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { Note } from '../../sonar-aligned';
import { Highlight } from '../Text';
import { RequiredIcon } from '../icons';

interface Props {
  ariaLabel?: string;
  children: ReactNode;
  className?: string;
  description?: string | ReactNode;
  help?: ReactNode;
  htmlFor?: string;
  id?: string;
  label: string | ReactNode;
  required?: boolean;
  requiredAriaLabel?: string;
  title?: string;
}

export function FormField({
  children,
  className,
  description,
  help,
  id,
  required,
  label,
  htmlFor,
  title,
  ariaLabel,
  requiredAriaLabel,
}: Readonly<Props>) {
  return (
    <FieldWrapper className={className} id={id}>
      <Highlight className="sw-mb-2 sw-flex sw-items-center sw-gap-2">
        <StyledLabel aria-label={ariaLabel} htmlFor={htmlFor} title={title}>
          {label}
          {required && (
            <RequiredIcon aria-label={requiredAriaLabel ?? 'required'} className="sw-ml-1" />
          )}
        </StyledLabel>
        {help}
      </Highlight>

      {children}

      {description && <Note className="sw-mt-2">{description}</Note>}
    </FieldWrapper>
  );
}

// This is needed to prevent the target input/button from being focused
// when clicking/hovering on the label. More info https://stackoverflow.com/questions/9098581/why-is-hover-for-input-triggered-on-corresponding-label-in-css
const StyledLabel = styled(Label)`
  pointer-events: none;
`;

const FieldWrapper = styled.div`
  ${tw`sw-flex sw-flex-col sw-w-full`}

  &:not(:last-of-type) {
    ${tw`sw-mb-6`}
  }
`;
