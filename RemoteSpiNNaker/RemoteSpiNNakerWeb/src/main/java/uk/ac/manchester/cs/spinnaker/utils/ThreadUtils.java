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
package uk.ac.manchester.cs.spinnaker.utils;

/**
 * Utilities for working with threads.
 */
public abstract class ThreadUtils {

    /**
     * Avoid creation.
     */
    private ThreadUtils() {
    }

    /**
     * Recommended way of doing "quiet" sleeps.
     *
     * @param delay
     *            How long to sleep for, in milliseconds.
     * @see <a href="https://stackoverflow.com/q/1087475/301832">Stack Overflow
     *      Question: When does Java's Thread.sleep throw
     *      InterruptedException?</a>
     */
    public static void sleep(final long delay) {
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait for the given object.
     *
     * @param obj
     *            The object to wait for
     * @return True if the wait was interrupted, false otherwise
     */
    public static boolean waitfor(final Object obj) {
        try {
            obj.wait();
            return false;
        } catch (final InterruptedException e) {
            return true;
        }
    }

    /**
     * Wait for the given object.
     *
     * @param obj
     *            The object to wait for
     * @param timeout
     *            The maximum time to wait, in milliseconds
     * @return True if the wait was interrupted, false otherwise
     */
    public static boolean waitfor(final Object obj, final long timeout) {
        try {
            obj.wait(timeout);
            return false;
        } catch (final InterruptedException e) {
            return true;
        }
    }
}
