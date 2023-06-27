/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { Highlight, Note } from '../Text';
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
}: Props) {
  return (
    <FieldWrapper className={className} id={id}>
      <label aria-label={ariaLabel} className="sw-mb-2" htmlFor={htmlFor} title={title}>
        <Highlight className="sw-flex sw-items-center sw-gap-2">
          {label}
          {required && <RequiredIcon className="sw--ml-1" />}
          {help}
        </Highlight>
      </label>

      {children}

      {description && <Note className="sw-mt-2">{description}</Note>}
    </FieldWrapper>
  );
}

const FieldWrapper = styled.div`
  ${tw`sw-flex sw-flex-col sw-w-full`}

  &:not(:last-of-type) {
    ${tw`sw-mb-6`}
  }
`;
