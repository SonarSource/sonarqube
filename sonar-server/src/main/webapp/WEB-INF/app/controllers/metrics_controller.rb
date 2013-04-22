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
class MetricsController < ApplicationController

  before_filter :admin_required

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    prepare_metrics_and_domains
    
    if params['id']
      @metric=Metric.find(params['id'].to_i)
      params['domain']=@metric.domain(false)
    else
      @metric=Metric.new
    end
    render :action => 'index'
  end

  def save_from_web
    short_name = params[:metric][:short_name]
    metric_name = short_name.downcase.gsub(/\s/, '_')[0..59]
    
    if params[:id]
      metric = Metric.find(params[:id].to_i)
    else
      metric = Metric.first(:conditions => ["name = ?", metric_name])
      if metric
        @reactivate_metric = metric
      else
        metric = Metric.new
      end
    end

    metric.attributes=params[:metric]
    if metric.short_name(false)
      metric.name = metric.short_name(false).downcase.gsub(/\s/, '_')[0..59] unless params[:id]
    end
    unless params[:newdomain].blank?
      metric.domain = params[:newdomain]
    end
    metric.direction = 0
    metric.user_managed = true
    metric.origin = Metric::ORIGIN_GUI
    metric.enabled = true unless @reactivate_metric

    begin
      new_rec = metric.new_record?
      metric.save!
      unless @reactivate_metric
        Metric.clear_cache
        if new_rec
          flash[:notice] = 'Successfully created.'
        else
          flash[:notice] = 'Successfully updated.'
        end
      end
    rescue
      flash[:error] = metric.errors.full_messages.join("<br/>\n")
    end
    
    if @reactivate_metric
      prepare_metrics_and_domains
      render :action => 'index'
    else
      redirect_to :action => 'index', :domain => metric.domain(false)
    end
  end

  def reactivate
    begin
      metric = Metric.find(params[:id].to_i)
      metric.enabled = true
      metric.save!
      Metric.clear_cache
      flash[:notice] = 'Successfully reactivated.'
    rescue
      flash[:error] = metric.errors.full_messages.join("<br/>\n")
    end
    redirect_to :action => 'index', :domain => metric.domain(false)
  end

  def delete_from_web
    metric = Metric.by_id(params[:id].to_i) if params[:id] && params[:id].size > 0
    if metric
      Metric.delete_with_manual_measures(params[:id].to_i)
      flash[:notice] = 'Successfully deleted.'
      Metric.clear_cache
    end
    redirect_to :action => 'index'
  end
  
  private
  
  def prepare_metrics_and_domains
    @metrics = Metric.all.select { |metric| metric.user_managed? }
    @domains = Metric.all.map { |metric| metric.domain(false) }.compact.uniq.sort
  end
  
end
