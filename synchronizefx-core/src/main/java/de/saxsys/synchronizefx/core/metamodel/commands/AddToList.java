/**
 * This file is part of SynchronizeFX.
 * 
 * Copyright (C) 2013-2014 Saxonia Systems AG
 *
 * SynchronizeFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SynchronizeFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SynchronizeFX. If not, see <http://www.gnu.org/licenses/>.
 */

package de.saxsys.synchronizefx.core.metamodel.commands;

import java.util.UUID;

/**
 * A command that states that an element should be added to a list.
 * 
 * @author raik.bieniek
 * 
 */
public class AddToList implements Command {

    private UUID listId;
    private Value value;
    private int position;
    private int newSize;

    /**
     * @return The id of the list where an element should be added.
     */
    public UUID getListId() {
        return listId;
    }

    /**
     * @see AddToList#getListId()
     * @param listId the id
     */
    public void setListId(final UUID listId) {
        this.listId = listId;
    }

    /**
     * @return The value to add to the list. 
     */
    public Value getValue() {
        return value;
    }

    /**
     * @see #getValue()
     * @param value the value
     */
    public void setValue(final Value value) {
        this.value = value;
    }

    /**
     * @return The index the new element will have in the list when it's added. The index of all items in the list thats
     *         index is greater or equal to the value returned here has to be incremented by 1 to make this index
     *         available.
     */
    public int getPosition() {
        return position;
    }

    /**
     * The new size the list should have after this command has been executed on it.
     * 
     * @return the new size
     */
    public int getNewSize() {
        return newSize;
    }

    /**
     * @see RemoveFromList#getNewSize()
     * @param newSize the new size
     */
    public void setNewSize(final int newSize) {
        this.newSize = newSize;
    }
    
    /**
     * @see AddToList#getPosition()
     * @param position the position
     */
    public void setPosition(final int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "AddToList [listId=" + listId + ", value=" + value + ", position=" + position + ", newSize=" + newSize
                + "]";
    }
}
