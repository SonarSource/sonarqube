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
class DependenciesController < ApplicationController

  SECTION=Navigation::SECTION_HOME

  SEARCH_MINIMUM_SIZE=3
  QUALIFIERS=['TRK', 'BRC', 'LIB']

  def index
    @search=params[:search] || ''
    @version=params[:version]
    @resources=nil
    @resource=nil
    @versions=nil

    if @search.size>0 && @search.size<SEARCH_MINIMUM_SIZE
      flash[:notice]="Minimum search is #{SEARCH_MINIMUM_SIZE} characters."
      redirect_to :action => 'index'
      return
    end

    if @search.size>=SEARCH_MINIMUM_SIZE

      #
      # load all the resources (first column)
      #
      @resources=Project.find(:all,
        :conditions => ["scope=? AND qualifier IN (?) AND enabled=? AND (UPPER(name) like ? OR kee like ?)", 'PRJ', QUALIFIERS, true, "%#{@search.upcase}%", "%#{@search}%"])
      @resources = select_authorized(:user, @resources)

      Api::Utils.insensitive_sort!(@resources){|r| r.name}

      if params[:resource]
        @resource=@resources.select{|r| r.kee==params[:resource]}.first
      elsif @resources.size==1
        @resource=@resources.first
      end

    end

    if @resource

      #
      # load all the snapshots and versions of the selected resource (second column)
      #
      snapshots=Snapshot.find(:all, :select => 'id,version', :conditions => ['project_id=? AND version IS NOT NULL AND status=?', @resource.id, 'P'])
      @versions=snapshots.map{|s| s.version}.compact.uniq.sort.reverse
      @version=@versions.first if @version.blank? && @versions.size==1


      #
      # load all the dependencies to the selected resource
      #
      conditions=["dependencies.from_scope='PRJ' AND snapshots.status='P' AND snapshots.project_id=:rid"]
      values={:rid => @resource.id}
      if !@version.blank?
        conditions<<'snapshots.version=:version'
        values[:version]=@version
      else
        conditions<<'snapshots.version IS NOT NULL'
      end
      deps=Dependency.find(:all,
        :include => 'to_snapshot',
        :select => 'dependencies.project_snapshot_id',
        :conditions => [conditions.join(' AND '), values])



      #
      # load all the projects defining the dependencies (third column)
      #
      @project_snapshots=[]
      snapshot_ids = deps.map{|dep| dep.project_snapshot_id}
      if snapshot_ids.size>0
        snapshot_ids.each_slice(999) do |safe_for_oracle_ids|
          @project_snapshots.concat(Snapshot.all(:include => 'project', :conditions => ['id IN (?) AND islast=? AND status=?', safe_for_oracle_ids, true, 'P']))
        end
        @project_snapshots = select_authorized(:user, @project_snapshots)
        Api::Utils.insensitive_sort!(@project_snapshots) {|s| s.project.name}
      end

    end

  end


end
