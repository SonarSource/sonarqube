#
# Sonar, entreprise quality control tool.
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
class MetricsController < ApplicationController

  before_filter :admin_required

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    @metrics = Metric.all.select { |metric| metric.user_managed? }
    @domains = Metric.all.map { |metric| metric.domain(false) }.compact.uniq.sort
    if params['id']
      @metric=Metric.find(params['id'].to_i)
      params['domain']=@metric.domain(false)
    else
      @metric=Metric.new
    end
    render :action => 'index'
  end

  def save_from_web
    if params[:id]
      metric = Metric.find(params[:id].to_i)
    else
      metric = Metric.new
    end

    metric.attributes=params[:metric]
    if metric.short_name(false)
      metric.name = metric.short_name(false).downcase.gsub(/\s/, '_')[0..59]
    end
    unless params[:newdomain].blank?
      metric.domain = params[:newdomain]
    end
    metric.direction = 0
    metric.user_managed = true
    metric.enabled = true
    metric.origin = Metric::ORIGIN_GUI

    begin
      new_rec = metric.new_record?
      metric.save!
      Metric.clear_cache
      if new_rec
        flash[:notice] = 'Successfully created.'
      else
        flash[:notice] = 'Successfully updated.'
      end
    rescue
      flash[:error] = metric.errors.full_messages.join("<br/>\n")
    end
    redirect_to :action => 'index', :domain => metric.domain(false)
  end

  def delete_from_web
    metric = Metric.by_id(params[:id].to_i) if params[:id] && params[:id].size > 0
    if metric
      del_count = Metric.delete(params[:id].to_i)
      flash[:notice] = 'Successfully deleted.' if del_count == 1
      flash[:error] = 'Unable to delete this metric.' if del_count != 1
      Metric.clear_cache
    end
    redirect_to :action => 'index'
  end
end
