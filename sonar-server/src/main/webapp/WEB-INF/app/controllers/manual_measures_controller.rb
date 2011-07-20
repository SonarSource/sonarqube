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
class ManualMeasuresController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :load_resource
  verify :method => :post, :only => [:save, :delete], :redirect_to => { :action => :index }

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
    metric=Metric.by_key(params[:metric])
    measure=ManualMeasure.find(:first, :conditions => ['resource_id=? and metric_id=?', @resource.id, metric.id])
    if measure.nil?
      measure=ManualMeasure.new(:resource => @resource, :user_login => current_user.login, :metric_id => metric.id)
    end
    # TODO use measure.text_value if string metric
    measure.value = params[:val]
    measure.description = params[:desc]
    measure.save!
    redirect_to :action => 'index', :resource => params[:resource], :metric => params[:metric]
  end

  def delete
    metric=Metric.by_key(params[:metric])
    ManualMeasure.destroy_all(['resource_id=? and metric_id=?', @resource.id, metric.id])
    redirect_to :action => 'index', :resource => params[:resource], :metric => params[:metric]
  end

  private

  def load_resource
    @resource=Project.by_key(params[:resource])
    return redirect_to home_path unless @resource
    return access_denied unless has_role?(:admin, @resource)
  end

  def load_measures
    @measures=ManualMeasure.find(:all, :conditions => ['resource_id=?', @resource.id]).select{|m| m.metric.enabled}.sort_by{|m| m.metric.domain}
  end
end
