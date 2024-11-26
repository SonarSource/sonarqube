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
import { Heading } from '@sonarsource/echoes-react';

interface AiCodeAssuranceBannerProps {
  className?: string;
  description: React.ReactNode;
  icon: React.ReactNode;
  title: React.ReactNode;
}

function AiCodeAssuranceBanner({
  className,
  icon,
  title,
  description,
}: Readonly<AiCodeAssuranceBannerProps>) {
  return (
    <StyledWrapper className={className}>
      <MessageContainer>
        <LeftContent>
          {icon}
          <TextWrapper>
            <PromotedHeading as="h3">{title}</PromotedHeading>
            {description}
          </TextWrapper>
        </LeftContent>
      </MessageContainer>
    </StyledWrapper>
  );
}

export default AiCodeAssuranceBanner;

const StyledWrapper = styled.div`
  background-color: var(--echoes-color-background-accent-weak-default);
  border: 1px solid var(--echoes-color-border-weak);
  padding-left: var(--echoes-dimension-space-300);
  border-radius: var(--echoes-border-radius-400);
`;

const MessageContainer = styled.div`
  padding-top: var(--echoes-dimension-space-100);
  padding-bottom: var(--echoes-dimension-space-100);
`;

const LeftContent = styled.div`
  display: flex;
  align-items: center;
  gap: var(--echoes-border-radius-400);
`;

const TextWrapper = styled.div`
  display: flex;
  flex-direction: column;
  padding-top: var(--echoes-dimension-space-100);
  padding-bottom: var(--echoes-dimension-space-100);
  gap: var(--echoes-border-radius-400);
`;

const PromotedHeading = styled(Heading)`
  color: var(--echoes-color-text-accent-bold);
`;
