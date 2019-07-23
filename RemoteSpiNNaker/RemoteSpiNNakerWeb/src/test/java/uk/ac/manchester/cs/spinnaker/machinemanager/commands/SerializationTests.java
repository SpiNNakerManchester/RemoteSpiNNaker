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
package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("rawtypes")
public class SerializationTests {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testCreateJob() throws JsonProcessingException {
        final Command cmd = new CreateJobCommand(123, "abc def");
        assertEquals(
                "{\"command\":\"create_job\",\"args\":[123],"
                + "\"kwargs\":{\"owner\":\"abc def\"}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testDestroyJob() throws JsonProcessingException {
        final Command cmd = new DestroyJobCommand(123);
        assertEquals(
                "{\"command\":\"destroy_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testGetJobMachineInfo() throws JsonProcessingException {
        final Command cmd = new GetJobMachineInfoCommand(123);
        assertEquals(
                "{\"command\":\"get_job_machine_info\",\"args\":[123],"
                + "\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testGetJobState() throws JsonProcessingException {
        final Command cmd = new GetJobStateCommand(123);
        assertEquals(
                "{\"command\":\"get_job_state\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testJobKeepAlive() throws JsonProcessingException {
        final Command cmd = new JobKeepAliveCommand(123);
        assertEquals(
                "{\"command\":\"job_keepalive\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testListMachines() throws JsonProcessingException {
        final Command cmd = new ListMachinesCommand();
        assertEquals(
                "{\"command\":\"list_machines\",\"args\":[],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testNoNotifyJob() throws JsonProcessingException {
        final Command cmd = new NoNotifyJobCommand(123);
        assertEquals(
                "{\"command\":\"no_notify_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testNotifyJob() throws JsonProcessingException {
        final Command cmd = new NotifyJobCommand(123);
        assertEquals(
                "{\"command\":\"notify_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }
}
