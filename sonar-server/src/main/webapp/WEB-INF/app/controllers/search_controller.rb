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
class SearchController < ApplicationController

  SECTION=Navigation::SECTION_HOME

  # Do not exceed 1000 because of the Oracle limition on IN statements
  MAX_RESULTS = 6

  MIN_SEARCH_SIZE=3

  def index
    @start_time = Time.now
    search = params[:s]
    bad_request('Minimum search is 3 characters') if search.empty? || search.to_s.size<MIN_SEARCH_SIZE

    key = search.downcase
    results = ResourceIndex.find(:all,
                                 :select => 'resource_id,root_project_id,qualifier', # optimization to not load unused columns like 'kee'
                                 :conditions => ["kee like ?", key + '%'],
                                 :order => 'name_size')

    results = select_authorized(:user, results)
    @total = results.size

    resource_ids=[]
    @results_by_qualifier={}
    results.each do |resource_index|
      qualifier = fix_qualifier(resource_index.qualifier)
      @results_by_qualifier[qualifier]||=[]
      array=@results_by_qualifier[qualifier]
      if array.size<MAX_RESULTS
        resource_ids<<resource_index.resource_id
        array<<resource_index
      end
    end

    @resources_by_id = {}
    unless resource_ids.empty?
      Project.find(:all, :conditions => ['id in (?)', resource_ids]).each do |resource|
        @resources_by_id[resource.id]=resource
      end
    end

    render :partial => 'search/autocomplete'
  end

  private
  def fix_qualifier(q)
    case q
      when 'CLA' then
        'FIL'
      else
        q
    end
  end
end
