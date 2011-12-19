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
  MAX_RESULTS = 50

  def index
    @start_time = Time.now
    @search = params[:s]
    unless @search.empty?
      if @search.to_s.size>=3
        normalized_search = @search.downcase
        @results = ResourceIndex.find(:all,
                                      :conditions => ["resource_index.kee like ?", normalized_search + '%'],
                                      :order => 'name_size, position')

        @results = select_authorized(:user, @results)
        @total = @results.size
        @results = @results[0...MAX_RESULTS]

        @resources_by_id = {}
        unless @results.empty?
          Project.find(:all, :conditions => ['id in (?)', @results.map { |resource_index| resource_index.resource_id }]).each do |resource|
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
