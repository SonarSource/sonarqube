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

import * as React from 'react';
import { createCodefixPr } from '../../api/codefix';
import { CreatePRButton, CreatePRIcon, CreatePRText } from './fixDiffStyles';

interface CreatePullRequestButtonProps {
  jobId?: string;
}

export function CreatePullRequestButton({ jobId }: Readonly<CreatePullRequestButtonProps>) {
  const [isActive, setIsActive] = React.useState(false);
  const [isDisabled, setIsDisabled] = React.useState(false);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const handleCreatePR = React.useCallback(() => {
    if (!jobId || isDisabled) return;
    setIsSubmitting(true);
    createCodefixPr(jobId)
      .then(() => {
        setIsActive(true);
        setIsDisabled(true);
      })
      .catch(() => {
        // Error already handled by request layer; keep button clickable for retry
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  }, [jobId, isDisabled]);

  const disabled = !jobId || isDisabled || isSubmitting;

  return (
    <CreatePRButton
      type="button"
      onClick={handleCreatePR}
      disabled={disabled}
      $active={isActive}
    >
      <CreatePRIcon>
        <svg width="26" height="26" viewBox="0 0 26 26" fill="none" xmlns="http://www.w3.org/2000/svg">
          <g filter="url(#filter0_d_569_126)">
            <rect x="4.5" y="0.5" width="17" height="17" stroke="black" shapeRendering="crispEdges" />
            <path
              d="M11.3125 4.50003C11.3123 4.07925 11.1941 3.66696 10.9713 3.30999C10.7486 2.95302 10.4302 2.66566 10.0523 2.48056C9.67441 2.29546 9.2522 2.22003 8.83361 2.26283C8.41501 2.30564 8.01681 2.46497 7.68422 2.72273C7.35163 2.98048 7.09798 3.32634 6.95209 3.72101C6.80619 4.11569 6.77389 4.54336 6.85885 4.95547C6.94381 5.36758 7.14263 5.74761 7.43273 6.0524C7.72283 6.35719 8.09259 6.57452 8.50001 6.67971V11.3203C7.96976 11.4572 7.50765 11.7828 7.20029 12.2361C6.89294 12.6893 6.76143 13.2391 6.83044 13.7824C6.89944 14.3257 7.16421 14.8251 7.57512 15.1871C7.98603 15.5492 8.51487 15.7489 9.06251 15.7489C9.61014 15.7489 10.139 15.5492 10.5499 15.1871C10.9608 14.8251 11.2256 14.3257 11.2946 13.7824C11.3636 13.2391 11.2321 12.6893 10.9247 12.2361C10.6174 11.7828 10.1553 11.4572 9.62501 11.3203V6.67971C10.1078 6.55431 10.5354 6.2723 10.8408 5.87787C11.1461 5.48343 11.312 4.99885 11.3125 4.50003ZM10.1875 13.5C10.1875 13.7225 10.1215 13.94 9.99791 14.125C9.87429 14.31 9.69859 14.4542 9.49302 14.5394C9.28746 14.6245 9.06126 14.6468 8.84303 14.6034C8.6248 14.56 8.42434 14.4529 8.26701 14.2955C8.10968 14.1382 8.00253 13.9377 7.95912 13.7195C7.91571 13.5013 7.93799 13.2751 8.02314 13.0695C8.10829 12.8639 8.25248 12.6882 8.43749 12.5646C8.62249 12.441 8.84 12.375 9.06251 12.375C9.36087 12.375 9.64702 12.4936 9.858 12.7045C10.069 12.9155 10.1875 13.2017 10.1875 13.5ZM9.06251 5.62503C8.84 5.62503 8.62249 5.55905 8.43749 5.43543C8.25248 5.31181 8.10829 5.13611 8.02314 4.93055C7.93799 4.72498 7.91571 4.49878 7.95912 4.28055C8.00253 4.06232 8.10968 3.86187 8.26701 3.70453C8.42434 3.5472 8.6248 3.44005 8.84303 3.39664C9.06126 3.35323 9.28746 3.37551 9.49302 3.46066C9.69859 3.54581 9.87429 3.69 9.99791 3.87501C10.1215 4.06002 10.1875 4.27752 10.1875 4.50003C10.1875 4.79839 10.069 5.08454 9.858 5.29552C9.64702 5.5065 9.36087 5.62503 9.06251 5.62503ZM18.625 11.3203V8.71034C18.6263 8.19306 18.5251 7.68065 18.3272 7.20273C18.1292 6.72481 17.8385 6.29086 17.4719 5.92596L15.4827 3.93753H17.5C17.6492 3.93753 17.7923 3.87826 17.8978 3.77277C18.0032 3.66728 18.0625 3.52421 18.0625 3.37503C18.0625 3.22584 18.0032 3.08277 17.8978 2.97728C17.7923 2.87179 17.6492 2.81253 17.5 2.81253H14.125C13.9758 2.81253 13.8327 2.87179 13.7273 2.97728C13.6218 3.08277 13.5625 3.22584 13.5625 3.37503V6.75003C13.5625 6.89921 13.6218 7.04228 13.7273 7.14777C13.8327 7.25326 13.9758 7.31253 14.125 7.31253C14.2742 7.31253 14.4173 7.25326 14.5228 7.14777C14.6282 7.04228 14.6875 6.89921 14.6875 6.75003V4.73276L16.6759 6.7219C16.938 6.98233 17.1458 7.29219 17.2873 7.63352C17.4287 7.97485 17.501 8.34086 17.5 8.71034V11.3203C16.9698 11.4572 16.5076 11.7828 16.2003 12.2361C15.8929 12.6893 15.7614 13.2391 15.8304 13.7824C15.8994 14.3257 16.1642 14.8251 16.5751 15.1871C16.986 15.5492 17.5149 15.7489 18.0625 15.7489C18.6101 15.7489 19.139 15.5492 19.5499 15.1871C19.9608 14.8251 20.2256 14.3257 20.2946 13.7824C20.3636 13.2391 20.2321 12.6893 19.9247 12.2361C19.6174 11.7828 19.1553 11.4572 18.625 11.3203ZM18.0625 14.625C17.84 14.625 17.6225 14.559 17.4375 14.4354C17.2525 14.3118 17.1083 14.1361 17.0231 13.9305C16.938 13.725 16.9157 13.4988 16.9591 13.2806C17.0025 13.0623 17.1097 12.8619 17.267 12.7045C17.4243 12.5472 17.6248 12.4401 17.843 12.3966C18.0613 12.3532 18.2875 12.3755 18.493 12.4607C18.6986 12.5458 18.8743 12.69 18.9979 12.875C19.1215 13.06 19.1875 13.2775 19.1875 13.5C19.1875 13.7984 19.069 14.0845 18.858 14.2955C18.647 14.5065 18.3609 14.625 18.0625 14.625Z"
              fill="white"
            />
          </g>
          <defs>
            <filter
              id="filter0_d_569_126"
              x="0"
              y="0"
              width="26"
              height="26"
              filterUnits="userSpaceOnUse"
              colorInterpolationFilters="sRGB"
            >
              <feFlood floodOpacity="0" result="BackgroundImageFix" />
              <feColorMatrix
                in="SourceAlpha"
                type="matrix"
                values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0"
                result="hardAlpha"
              />
              <feOffset dy="4" />
              <feGaussianBlur stdDeviation="2" />
              <feComposite in2="hardAlpha" operator="out" />
              <feColorMatrix type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.25 0" />
              <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_569_126" />
              <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_569_126" result="shape" />
            </filter>
          </defs>
        </svg>
      </CreatePRIcon>
      <CreatePRText>Create Pull Request</CreatePRText>
    </CreatePRButton>
  );
}

