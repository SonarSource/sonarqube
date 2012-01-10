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
class AdminFiltersController < ApplicationController
  include FiltersHelper

  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => [:up, :down, :remove, :add], :redirect_to => {:action => :index}
  before_filter :admin_required
  before_filter :load_active_filters

  def index
    @shared_filters=::Filter.find(:all, :conditions => {:shared => true}).sort{|a,b| a.name.downcase<=>b.name.downcase}
    ids=@actives.map{|af| af.filter_id}
    @shared_filters.reject!{|f| ids.include?(f.id) }
  end

  def up
    active_index=-1
    active=nil
    @actives.each_index do |index|
      if @actives[index].id==params[:id].to_i
        active_index=index
        active=@actives[index]
      end
    end
    if active && active_index>0
      @actives[active_index]=@actives[active_index-1]
      @actives[active_index-1]=active

      @actives.each_index do |index|
        @actives[index].order_index=index+1
        @actives[index].save
      end
    end
    redirect_to :action => 'index'
  end

  def down
    filter_index=-1
    filter=nil
    @actives.each_index do |index|
      if @actives[index].id==params[:id].to_i
        filter_index=index
        filter=@actives[index]
      end
    end
    if filter && filter_index<@actives.size-1
      @actives[filter_index]=@actives[filter_index+1]
      @actives[filter_index+1]=filter

      @actives.each_index do |index|
        @actives[index].order_index=index+1
        @actives[index].save
      end
    end
    redirect_to :action => 'index'
  end

  def add
    filter=::Filter.find(:first, :conditions => ['shared=? and id=?', true, params[:id].to_i])
    if filter
      ActiveFilter.create(:filter => filter, :user => nil, :order_index => @actives.size+1)
      flash[:notice]='Default filter added.'
    end
    redirect_to :action => 'index'
  end

  def remove
    active=@actives.to_a.find{|af| af.id==params[:id].to_i}
    if active
      active.destroy
      flash[:notice]='Filter removed from default filters.'
    end
    redirect_to :action => 'index'
  end

  private

  def load_active_filters
    @actives=ActiveFilter.default_active_filters
  end
end
