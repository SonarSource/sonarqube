#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
require 'set'
class IssueFilter

  attr_reader :paging, :issues, :issues_result

  def criteria(key=nil)
    @criteria ||= HashWithIndifferentAccess.new
    if key
      @criteria[key]
    else
      @criteria
    end
  end

  def criteria=(hash)
    @criteria = HashWithIndifferentAccess.new
    hash.each_pair do |k, v|
      set_criteria_value(k, v)
    end
  end

  def override_criteria(hash)
    @criteria ||= HashWithIndifferentAccess.new
    hash.each_pair do |k, v|
      set_criteria_value(k, v)
    end
  end

  # API used by Displays
  def set_criteria_value(key, value)
    @criteria ||= HashWithIndifferentAccess.new
    if key
      if value!=nil && value!='' && value!=['']
        value = (value.kind_of?(Array) ? value : value.to_s)
        @criteria[key]=value
      else
        @criteria.delete(key)
      end
    end
  end

  def execute
    init_results
    @issues_result = Api.issues.find(criteria)
    @paging = @issues_result.paging
    @issues = @issues_result.issues
    self
  end

  private

  def init_results
    @issues_result = nil
    @paging = nil
    @issues = nil
    criteria['pageSize'] = 100
    self
  end

end