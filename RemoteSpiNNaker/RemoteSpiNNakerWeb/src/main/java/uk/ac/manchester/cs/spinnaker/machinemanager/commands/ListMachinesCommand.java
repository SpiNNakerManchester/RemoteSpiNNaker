/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

/**
 * Request to get the known machines from the spalloc service.
 */
public class ListMachinesCommand extends Command<String> {
    /** Create a request to list the known SpiNNaker machines. */
    public ListMachinesCommand() {
        super("list_machines");
    }
}
