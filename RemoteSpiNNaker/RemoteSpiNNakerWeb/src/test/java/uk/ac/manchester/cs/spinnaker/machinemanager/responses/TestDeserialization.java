/*
 * Copyright (c) 2014-2023 The University of Manchester
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
package uk.ac.manchester.cs.spinnaker.machinemanager.responses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDeserialization {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void machine()
            throws JsonParseException, JsonMappingException, IOException {
        final var machine =
                "{\"name\":\"foo\",\"tags\":[\"a\",\"b c\"],\"width\":123,"
                + "\"height\":456}";
        final var m = mapper.readValue(machine, Machine.class);
        assertNotNull(m);

        assertEquals("foo", m.getName());
        assertEquals(123, m.getWidth());
        assertEquals(456, m.getHeight());
        assertEquals("[a, b c]", m.getTags().toString());
    }

    @Test
    public void machineArray()
            throws JsonParseException, JsonMappingException, IOException {
        final var machine =
                "[{\"name\":\"foo\",\"tags\":[\"a\",\"b c\"],\"width\":123,"
                + "\"height\":456},{\"name\":\"bar\",\"width\":1,"
                + "\"height\":2}]";
        final var m = mapper.readValue(machine, Machine[].class);
        assertNotNull(m);

        assertEquals(2, m.length);
        assertEquals("foo", m[0].getName());
        assertEquals(123, m[0].getWidth());
        assertEquals(456, m[0].getHeight());
        assertEquals("[a, b c]", m[0].getTags().toString());

        assertEquals("bar", m[1].getName());
        assertEquals(1, m[1].getWidth());
        assertEquals(2, m[1].getHeight());
        assertNull(m[1].getTags());
    }

    @Test
    public void jobMachineInfo()
            throws JsonParseException, JsonMappingException, IOException {
        final var machine = "{\"width\":123,\"height\":456,\"connections\":["
                + "[[1,2],\"abcde\"],[[3,4],\"edcba\"]"
                + "],\"machineName\":\"foo\"}";
        final var m = mapper.readValue(machine, JobMachineInfo.class);
        assertNotNull(m);

        assertEquals("foo", m.getMachineName());
        assertEquals(123, m.getWidth());
        assertEquals(456, m.getHeight());
        assertEquals(2, m.getConnections().size());

        assertEquals("abcde", m.getConnections().get(0).getHostname());
        assertEquals(1, m.getConnections().get(0).getChip().getX());
        assertEquals(2, m.getConnections().get(0).getChip().getY());

        assertEquals("edcba", m.getConnections().get(1).getHostname());
        assertEquals(3, m.getConnections().get(1).getChip().getX());
        assertEquals(4, m.getConnections().get(1).getChip().getY());
    }

    @Test
    public void jobState()
            throws JsonParseException, JsonMappingException, IOException {
        final var machine =
                "{\"state\":2,\"power\":true,\"keepAlive\":1.25,"
                + "\"reason\":\"foo\"}";
        final var m = mapper.readValue(machine, JobState.class);
        assertNotNull(m);

        assertEquals("foo", m.getReason());
        assertEquals(2, m.getState());
        assertEquals(true, m.getPower());
        // Hack to work around deprecation of comparison of doubles
        assertEquals(1250000, (int) (m.getKeepAlive() * 1000000));
    }
}
