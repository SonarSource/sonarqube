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
package org.sonar.db.report;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

public interface ReportSubscriptionMapper {
  Optional<ReportSubscriptionDto> selectByUserAndPortfolio(@Param("portfolioUuid") String portfolioUuid, @Param("userUuid") String userUuid);

  Optional<ReportSubscriptionDto> selectByUserAndBranch(@Param("branchUuid") String branchUuid, @Param("userUuid") String userUuid);

  List<ReportSubscriptionDto> selectByPortfolio(String portfolioUuid);

  List<ReportSubscriptionDto> selectByBranch(String projectBranchUuid);

  Set<ReportSubscriptionDto> selectAll();

  void insert(ReportSubscriptionDto subscriptionDto);

  void delete(ReportSubscriptionDto subscriptionDto);
}
