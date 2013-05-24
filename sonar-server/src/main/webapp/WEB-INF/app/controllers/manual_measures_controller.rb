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
class ManualMeasuresController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :init_resource_for_admin_role
  helper MetricsHelper
  
  def index
    load_measures()
  end

  def new
    if params[:metric].present?
      @metric=Metric.by_key(params[:metric])
      @measure=ManualMeasure.find(:first, :conditions => ['resource_id=? and metric_id=?', @resource.id, @metric.id]) || ManualMeasure.new
    else
      @metric=nil
      @measure=nil
    end
  end

  def save
    verify_post_request
    @metric=Metric.by_key(params[:metric])
    @measure=ManualMeasure.find(:first, :conditions => ['resource_id=? and metric_id=?', @resource.id, @metric.id])
    if @measure.nil?
      @measure=ManualMeasure.new(:resource => @resource, :user_login => current_user.login, :metric_id => @metric.id)
    end
    @measure.typed_value=params[:val]
    @measure.description=params[:desc]
    @measure.user_login=current_user.login
    if @measure.valid?
      @measure.save
      if (params[:redirect_to_new]=='true')
        redirect_to :action => 'new', :id => params[:id]
      else
        redirect_to :action => 'index', :id => params[:id], :metric => params[:metric]
      end
    else
      render :action => :new, :metric => @metric.id, :id => params[:id]
    end
  end

  def delete
    verify_post_request
    metric=Metric.by_key(params[:metric])
    ManualMeasure.destroy_all(['resource_id=? and metric_id=?', @resource.id, metric.id])
    redirect_to :action => 'index', :id => params[:id], :metric => params[:metric]
  end

  private

  def load_measures
    @measures=ManualMeasure.find(:all, :conditions => ['resource_id=?', @resource.id]).select { |m| m.metric.enabled }
  end
end
