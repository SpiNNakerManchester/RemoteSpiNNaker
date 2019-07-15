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
package uk.ac.manchester.cs.spinnaker.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stack trace provided remotely.
 */
public class RemoteStackTrace {

    /**
     * The elements of the stack trace.
     */
    private List<RemoteStackTraceElement> elements = new ArrayList<>();

    /**
     * Create a basic remote stack trace without stack elements.
     */
    public RemoteStackTrace() {
        // Does Nothing
    }

    /**
     * Create a basic remote stack trace from an exception.
     *
     * @param throwable
     *            The exception to make the stack trace from.
     */
    public RemoteStackTrace(final Throwable throwable) {
        for (final StackTraceElement element : throwable.getStackTrace()) {
            elements.add(new RemoteStackTraceElement(element));
        }
    }

    /**
     * Create a basic remote stack trace from a list of elements.
     *
     * @param elementsParam
     *            The elements to make the stack trace from.
     */
    public RemoteStackTrace(final List<RemoteStackTraceElement> elementsParam) {
        this.elements = elementsParam;
    }

    /**
     * Get the elements.
     *
     * @return the elements
     */
    public List<RemoteStackTraceElement> getElements() {
        return elements;
    }

    /**
     * Set the elements.
     *
     * @param elementsParam The elements to set
     */
    public void setElements(final List<RemoteStackTraceElement> elementsParam) {
        this.elements = elementsParam;
    }
}
