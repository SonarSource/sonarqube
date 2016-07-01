#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
# EXPERIMENTAL
#
class Api::TestsController < Api::ApiController

  # GET /api/tests/plan?resource=<test file id or key>
  #
  # Get the details of a given test plan :
  # - test cases
  # - resources covered by test cases
  #
  # Since v.3.5
  #
  # ==== Examples
  # - get the test plan defined by the file com/mycompany/FooTest.c of my_project : GET /api/tests/plan?resource=my_project:com/mycompany/FooTest.c
  #
  def plan
    require_parameters :resource

    resource=Project.by_key(params[:resource])
    not_found("Not found: #{params[:resource]}") unless resource
    access_denied unless has_role?(:user, resource)

    plan = java_facade.testPlan(resource.key)
    json = {}
    if plan
      resource = plan.component
      json[:key] = resource.key
      json[:name] = resource.name
      json[:longName] = resource.longName
      json[:testCases] = plan.testCases.map do |test_case|
        test_case_json = {:name => test_case.name}
        test_case_json[:type] = test_case.type
        test_case_json[:message] = test_case.message
        test_case_json[:durationInMs] = test_case.durationInMs
        test_case_json[:status] = test_case.status.to_s if test_case.status
        test_case_json[:stackTrace] = test_case.stackTrace
        if test_case.doesCover()
          test_case_json[:coverages] = test_case.coverageBlocks.map do |cover|
            cover_json = {}
            resource = cover.testable.component
            cover_json[:key] = resource.key
            cover_json[:name] = resource.name if resource.name
            cover_json[:longName] = resource.longName if resource.longName
            cover_json[:lines] = cover.lines if cover.lines
            cover_json.delete_if { |k, v| v.nil? }
            cover_json
          end
        end
        test_case_json.delete_if { |k, v| v.nil? }
        test_case_json
      end
      json.delete_if { |k, v| v.nil? }

      respond_to do |format|
        format.json { render :json => jsonp(json) }
        format.xml { render :xml => xml_not_supported }
        format.text { render :text => text_not_supported }
      end
    end
  end

  # GET /api/tests/testable?resource=<test file id or key>
  #
  # Get the details of a given test plan :
  # - test cases
  # - resources covered by test cases
  #
  # Since v.3.5
  #
  # ==== Examples
  # - get the test plan defined by the file com/mycompany/FooTest.c of my_project : GET /api/tests/plan?resource=my_project:com/mycompany/FooTest.c
  #
  def testable
    require_parameters :resource

    resource=Project.by_key(params[:resource])
    not_found("Not found: #{params[:resource]}") unless resource
    access_denied unless has_role?(:user, resource)

    testable = java_facade.testable(resource.key)
    json = {}
    if testable
      resource = testable.component
      json[:key] = resource.key
      json[:name] = resource.name if resource.name
      json[:longName] = resource.longName if resource.longName
      json[:coveredLines] = testable.testedLines

      json[:coverages] = testable.coverageBlocks.map do |coverage|
        test_case = coverage.testCase
        coverage_json = {}
        coverage_json[:lines] = coverage.lines
        coverage_json[:name] = test_case.name
        coverage_json[:status] = test_case.status.to_s if test_case.status
        coverage_json[:durationInMs] = test_case.durationInMs
        coverage_json[:testPlan] = test_case.testPlan.component.key

        coverage_json.delete_if { |k, v| v.nil? }
        coverage_json
      end

    end
    json.delete_if { |k, v| v.nil? }

    respond_to do |format|
      format.json { render :json => jsonp(json) }
      format.xml { render :xml => xml_not_supported }
      format.text { render :text => text_not_supported }
    end
  end
end
