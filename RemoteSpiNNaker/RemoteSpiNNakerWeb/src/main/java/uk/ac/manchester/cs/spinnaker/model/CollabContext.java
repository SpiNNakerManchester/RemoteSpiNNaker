/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.cs.spinnaker.model;

/**
 * POJO holding the description of a HBP Collaboratory context.
 */
public class CollabContext {

    /**
     * The ID of the APP in the collab.
     */
    private String appId;

    /**
     * The collab contained within.
     */
    private Collab collab;

    /**
     * The context string.
     */
    private String context;

    /**
     * The ID of the item.
     */
    private int id;

    /**
     * The name of the item.
     */
    private String name;

    /**
     * The order of the item within its group.
     */
    private int orderIndex;

    /**
     * The parent of the item.
     */
    private int parent;

    /**
     * The type of the item.
     */
    private String type;

    /**
     * Get the ID of the application.
     *
     * @return The ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Set the ID of the application.
     *
     * @param appIdParam The ID to set
     */
    public void setAppId(final String appIdParam) {
        this.appId = appIdParam;
    }

    /**
     * Get the collab of the application.
     *
     * @return The collab.
     */
    public Collab getCollab() {
        return collab;
    }

    /**
     * Set the collab of the application.
     *
     * @param collabParam The collab to set
     */
    public void setCollab(final Collab collabParam) {
        this.collab = collabParam;
    }

    /**
     * Get the context of the application.
     *
     * @return The context
     */
    public String getContext() {
        return context;
    }

    /**
     * Set the context of the application.
     *
     * @param contextParam The context to set
     */
    public void setContext(final String contextParam) {
        this.context = contextParam;
    }

    /**
     * Get the ID of the context.
     *
     * @return The ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the ID of the context.
     *
     * @param idParam The ID
     */
    public void setId(final int idParam) {
        this.id = idParam;
    }

    /**
     * Get the name of the context.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the context.
     *
     * @param nameParam The name to set
     */
    public void setName(final String nameParam) {
        this.name = nameParam;
    }

    /**
     * Get the index of this item to order.
     *
     * @return The index
     */
    public int getOrderIndex() {
        return orderIndex;
    }

    /**
     * Set the index of this item in order.
     *
     * @param orderIndexParam The index to set
     */
    public void setOrderIndex(final int orderIndexParam) {
        this.orderIndex = orderIndexParam;
    }

    /**
     * Get the parent of this item.
     *
     * @return The parent
     */
    public int getParent() {
        return parent;
    }

    /**
     * Set the parent of this item.
     *
     * @param parentParam The parent to set
     */
    public void setParent(final int parentParam) {
        this.parent = parentParam;
    }

    /**
     * Get the type of this item.
     *
     * @return The type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of this item.
     *
     * @param typeParam The type to set
     */
    public void setType(final String typeParam) {
        this.type = typeParam;
    }
}
