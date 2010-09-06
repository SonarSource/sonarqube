#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
class EventsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  # GET /events?rid=123
  # GET /events.xml?rid=123
  def index
    @resource=Project.by_key(params[:rid])
    return access_denied unless has_role?(:user, @resource)

    @events = Event.find(:all, :conditions => {:resource_id => @resource.id}, :order => 'created_at')

    respond_to do |format|
      format.html { render :layout => !request.xhr? }
      format.xml  { render :xml => @events }
    end
  end

  # GET /events/1
  # GET /events/1.xml
  def show
    @event = Event.find(params[:id])
    return access_denied unless has_role?(:user, @event.resource)
    respond_to do |format|
      format.html # show.html.erb
      format.xml  { render :xml => @event }
    end
  end

  # GET /events/new?rid=123
  # GET /events/new.xml?rid=123
  def new
    resource = Project.find(params[:rid])
    @event = Event.new(:resource => resource)
    if params[:sid]
      snapshot=Snapshot.find(params[:sid])
      @event.event_date=snapshot.created_at
    end

    @categories=EventCategory.categories(true)

    respond_to do |format|
      format.html { render :layout => !request.xhr? }
      format.xml  { render :xml => @event }
    end
  end

  # GET /events/1/edit
  def edit
    @event = Event.find(params[:id])
    @categories=EventCategory.categories(true)
    render :layout => !request.xhr?
  end

  # POST /events
  # POST /events.xml
  def create
    @event = Event.new(params[:event])
    return access_denied unless is_admin?(@event.resource)
    respond_to do |format|
      if @event.save
        flash[:notice] = 'Event is created.'
        format.html { redirect_to(:action => 'index') }
        format.xml  { render :xml => @event, :status => :created, :location => @event }
        format.js   # create.js.rjs
      else
        format.html { render :action => "new", :layout => !request.xhr? }
        format.xml  { render :xml => @event.errors, :status => :unprocessable_entity }
        format.js   # create.js.rjs
      end
    end
  end

  # PUT /events/1
  # PUT /events/1.xml
  def update
    @event = Event.find(params[:id])
    return access_denied unless is_admin?(@event.resource)
    respond_to do |format|
      if @event.update_attributes(params[:event])
        flash[:notice] = 'Event was successfully updated.'
        format.html { redirect_to(@event) }
        format.xml  { head :ok }
        format.js   # create.js.rjs
      else
        format.html { render :action => "edit" }
        format.xml  { render :xml => @event.errors, :status => :unprocessable_entity }
        format.js   # create.js.rjs
      end
    end
  end

  # DELETE /events/1
  # DELETE /events/1.xml
  def destroy
    @event = Event.find(params[:id])
    return access_denied unless is_admin?(@event.resource)
    @event.destroy
    flash[:notice] = 'Event is deleted.'

    respond_to do |format|
      format.html { redirect_to(:controller => 'project', :action => 'index', :id => @event.resource_id) }
      format.xml  { head :ok }
    end
  end


end
