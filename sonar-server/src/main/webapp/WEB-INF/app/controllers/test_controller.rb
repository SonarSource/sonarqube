#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class TestController < ApplicationController

  def testcase
    verify_ajax_request
    require_parameters :sid, :test
    snapshot_id = params[:sid].to_i

    @test = params[:test].to_s
    @test_plan = java_facade.testPlan(snapshot_id)
    @test_case = @test_plan.testCasesByName(@test).first
    render :partial => 'test/testcase'
  end

  def testable
    verify_ajax_request
    require_parameters :sid, :line
    snapshot_id = params[:sid].to_i

    @line = params[:line].to_i
    @testable = java_facade.testable(snapshot_id)
    @test_case_by_test_plan = {}
    @testable.testCasesOfLine(@line).each do |test_case|
      test_plan = test_case.testPlan
      test_cases = @test_case_by_test_plan[test_plan]
      test_cases = [] unless test_cases
      test_cases << test_case
      @test_case_by_test_plan[test_plan] = test_cases
    end
    render :partial => 'test/testable'
  end

end