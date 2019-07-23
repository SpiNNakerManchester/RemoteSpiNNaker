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
package uk.ac.manchester.cs.spinnaker.status;

/**
 * A service that handles status monitoring.
 */
public interface StatusMonitorManager {

    /**
     * Send a heart beat to the status monitoring service to indicate that we
     * are alive.
     *
     * @param runningJobs The number of running jobs.
     * @param nBoardsInUse The number of boards currently allocated.
     */
    void updateStatus(int runningJobs, int nBoardsInUse);
}
