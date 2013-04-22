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
require "set"

class SearchController < ApplicationController

  SECTION=Navigation::SECTION_HOME

  # Do not exceed 1000 because of the Oracle limitation on IN statements
  MAX_RESULTS = 6

  def index
    @start_time = Time.now
    search = params[:s]
    bad_request("Minimum search is #{ResourceIndex::MIN_SEARCH_SIZE} characters") if search.empty? || search.to_s.size<ResourceIndex::MIN_SEARCH_SIZE

    key = escape_like(search).downcase
    results = ResourceIndex.all(:select => 'distinct(resource_id),root_project_id,qualifier,name_size', # optimization to not load unused columns like 'kee'
                                 :conditions => ["kee like ?", key + '%'],
                                 :order => 'name_size')

    results = select_authorized(:user, results)
    @total = results.size

    resource_ids=[]
    @resource_indexes_by_qualifier={}
    results.each do |resource_index|
      qualifier = fix_qualifier(resource_index.qualifier)
      @resource_indexes_by_qualifier[qualifier] ||= []
      array = @resource_indexes_by_qualifier[qualifier]
      if array.size < MAX_RESULTS
        resource_ids << resource_index.resource_id
        array << resource_index
      end
    end

    @resources_by_id = {}
    unless resource_ids.empty?
      Project.find(:all, :conditions => ['id in (?) and enabled=?', resource_ids, true]).each do |resource|
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
