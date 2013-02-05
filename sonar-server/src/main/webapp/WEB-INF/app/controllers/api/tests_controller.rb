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
      json[:type] = plan.type
      json[:test_cases] = plan.testCases.map do |test_case|
        test_case_json = {:name => test_case.name}
        test_case_json[:message] = test_case.message if test_case.message
        test_case_json[:durationInMs] = test_case.durationInMs if test_case.durationInMs
        test_case_json[:status] = test_case.status.to_s if test_case.status
        test_case_json[:stackTrace] = test_case.stackTrace if test_case.stackTrace
        if test_case.doesCover()
          test_case_json[:covers] = test_case.covers.map do |cover|
            cover_json = {}
            resource = cover.testable.component
            cover_json[:resourceKey] = resource.key
            cover_json[:resourceName] = resource.name
            cover_json[:resourceQualifier] = resource.qualifier
            cover_json[:lines] = cover.lines
            cover_json
          end
        end
        test_case_json
      end

      respond_to do |format|
        format.json { render :json => jsonp(json) }
        format.xml { render :xml => xml_not_supported }
        format.text { render :text => text_not_supported }
      end
    end
  end
end