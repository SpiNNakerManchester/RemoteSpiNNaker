/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSerialization {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testCreateJob() throws JsonProcessingException {
        final var cmd = new CreateJobCommand(123, "abc def");
        assertEquals(
                "{\"command\":\"create_job\",\"args\":[123],"
                + "\"kwargs\":{\"owner\":\"abc def\"}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testDestroyJob() throws JsonProcessingException {
        final var cmd = new DestroyJobCommand(123);
        assertEquals(
                "{\"command\":\"destroy_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testGetJobMachineInfo() throws JsonProcessingException {
        final var cmd = new GetJobMachineInfoCommand(123);
        assertEquals(
                "{\"command\":\"get_job_machine_info\",\"args\":[123],"
                + "\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testGetJobState() throws JsonProcessingException {
        final var cmd = new GetJobStateCommand(123);
        assertEquals(
                "{\"command\":\"get_job_state\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testJobKeepAlive() throws JsonProcessingException {
        final var cmd = new JobKeepAliveCommand(123);
        assertEquals(
                "{\"command\":\"job_keepalive\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testListMachines() throws JsonProcessingException {
        final var cmd = new ListMachinesCommand();
        assertEquals(
                "{\"command\":\"list_machines\",\"args\":[],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testNoNotifyJob() throws JsonProcessingException {
        final var cmd = new NoNotifyJobCommand(123);
        assertEquals(
                "{\"command\":\"no_notify_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }

    @Test
    public void testNotifyJob() throws JsonProcessingException {
        final var cmd = new NotifyJobCommand(123);
        assertEquals(
                "{\"command\":\"notify_job\",\"args\":[123],\"kwargs\":{}}",
                mapper.writeValueAsString(cmd));
    }
}
