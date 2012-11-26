#
# Sonar, open source software quality management tool.
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
class SystemMeasureFiltersController < ApplicationController
  SECTION = Navigation::SECTION_CONFIGURATION

  # GET /system_measure_filters/index
  def index
    access_denied unless has_role?(:admin)

    @system_filters = MeasureFilter.find(:all, :include => :user, :conditions => ['system=?', true])
    Api::Utils.insensitive_sort!(@system_filters) { |f| f.name }

    @shared_filters = MeasureFilter.find(:all, :include => :user, :conditions => ['shared=? and system=?', true, false])
  end

  # POST /system_measure_filters/add/<filter id>
  def add
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :id

    filter = MeasureFilter.find(params[:id])
    filter.system=true
    unless filter.save
      flash[:error]=filter.errors.full_messages.join("<br/>")
    end
    redirect_to :action => :index
  end

  # POST /system_measure_filters/remove/<filter id>
  def remove
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :id

    filter = MeasureFilter.find(params[:id])
    filter.system=false
    unless filter.save
      flash[:error]=filter.errors.full_messages.join("<br/>")
    end
    redirect_to :action => :index
  end

end
