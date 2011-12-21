#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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

  verify :method => :post, :only => [:reset]
  before_filter :admin_required, :except => ['index']

  # Do not exceed 1000 because of the Oracle limition on IN statements
  MAX_RESULTS = 6

  def index
    @start_time = Time.now
    search = params[:s]
    unless search.blank?
      if search.to_s.size>=3
        key = search.downcase
        results = ResourceIndex.find(:all,
                                     :select => 'resource_id,root_project_id,qualifier', # optimization to not load unused columns like 'kee'
                                     :conditions => ["kee like ?", key + '%'],
                                     :order => 'name_size')

        results = select_authorized(:user, results)
        resource_ids=[]
        @results_by_qualifier={}
        @count_by_qualifier=Hash.new(0)
        results.each do |resource_index|
          @results_by_qualifier[resource_index.qualifier]||=[]
          array=@results_by_qualifier[resource_index.qualifier]
          if array.size<MAX_RESULTS
            resource_ids<<resource_index.resource_id
            array<<resource_index
          end
          @count_by_qualifier[resource_index.qualifier]+=1
        end

        @resources_by_id = {}
        unless resource_ids.empty?
          Project.find(:all, :conditions => ['id in (?)', resource_ids]).each do |resource|
            @resources_by_id[resource.id]=resource
          end
        end
      else
        flash[:warning]='Please refine your search'
      end
    end
  end

  # Start indexing resources
  #
  # curl -v -u admin:admin -X POST http://localhost:9000/search/reset
  def reset
    java_facade.indexResources()
    render :text => 'indexing'
  end
end
