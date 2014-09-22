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
class ManualMeasuresController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :init_resource_for_admin_role
  helper MetricsHelper
  
  def index
    load_measures()
  end

  def create_form
    load_measures()
    already_defined_metrics=@measures.map {|m| m.metric}
    @manual_metrics=Metric.all.select { |m| m.user_managed? && !already_defined_metrics.include?(m)}
    @metric=nil
    @measure=nil

    render :partial => 'manual_measures/create_form'
  end

  def edit_form
    @metric=Metric.by_key(params[:metric])
    @measure=ManualMeasure.find(:first, :conditions => ['resource_id=? and metric_id=?', @resource.id, @metric.id]) || ManualMeasure.new

    render :partial => 'manual_measures/edit_form'
  end

  def create
    verify_post_request
    if params[:metric]==''
      load_measures()
      already_defined_metrics=@measures.map {|m| m.metric}
      @manual_metrics=Metric.all.select { |m| m.user_managed? && !already_defined_metrics.include?(m)}
      render :text => 'Metric must be selected.', :status => 400

    else
      @metric=Metric.by_key(params[:metric])
      @measure=ManualMeasure.new(:resource => @resource, :user_login => current_user.login, :metric_id => @metric.id)

      @measure.typed_value=params[:val]
      @measure.description=params[:desc]
      @measure.user_login=current_user.login

      if @measure.valid?
        @measure.save
        flash[:notice] = 'Measure successfully created.'
        render :text => 'ok', :status => 200
      else
        render :text => @measure.errors.full_messages.map{|msg| msg}.join('<br/>'),
               :status => 400
      end
    end
  end

  def edit
    verify_post_request
    @metric=Metric.by_key(params[:metric])
    @measure=ManualMeasure.find(:first, :conditions => ['resource_id=? and metric_id=?', @resource.id, @metric.id])

    @measure.typed_value=params[:val]
    @measure.description=params[:desc]
    @measure.user_login=current_user.login

    if @measure.valid?
      @measure.save
      flash[:notice] = 'Measure successfully edited.'
      render :text => 'ok', :status => 200
    else
      render :text => @measure.errors.full_messages.map{|msg| msg}.join('<br/>'),
             :status => 400
    end
  end

  def delete
    verify_post_request
    metric=Metric.by_key(params[:metric])
    ManualMeasure.destroy_all(['resource_id=? and metric_id=?', @resource.id, metric.id])
    flash[:notice] = 'Measure successfully deleted.'
    redirect_to :action => 'index', :id => params[:id]
  end

  private

  def load_measures
    @measures=ManualMeasure.find(:all, :conditions => ['resource_id=?', @resource.id]).select { |m| m.metric.enabled }
  end
end
