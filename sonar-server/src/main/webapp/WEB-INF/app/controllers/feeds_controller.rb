#
# SonarQube, open source software quality management tool.
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
class FeedsController < ApplicationController
  
  FEEDS_LIMIT = 30
  MAX_FEEDS_LIMIT = 100

  def projects
    @category=params[:id]
    if @category
      @events =  Event.find(:all, :include => 'resource', :conditions => ['category=?', @category], :limit => feeds_count_limit, :order => "event_date desc")
    else
      @events =  Event.find(:all, :include => 'resource', :limit => feeds_count_limit, :order => "event_date desc")
    end

    @events=@events.select{|evt| evt.resource}

    @date=(@events.empty? ? Time.now : @events.first.event_date)
    respond_to do |format|
      format.atom
    end
  end
  
  def project
    @project=Project.by_key(params[:id])
    access_denied unless is_user?(@project)

    @category=params[:category]
    conditions={:resource_id => @project.id}
    conditions[:category]=@category if @category
    @events =  Event.find(:all, :include => 'resource', :conditions => conditions, :limit => feeds_count_limit, :order => "event_date desc")
    @date=(@events.empty? ? Time.now : @events.first.event_date)
    respond_to do |format|
      format.atom
    end
  end
  
  private
  def feeds_count_limit
    limit = params[:limit]
    limit.nil? ? FEEDS_LIMIT : [limit.to_i, MAX_FEEDS_LIMIT].max
  end
end
