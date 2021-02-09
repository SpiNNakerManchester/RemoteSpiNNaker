/*
 * Copyright (c) 2020 The University of Manchester
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

import static uk.ac.manchester.cs.spinnaker.rest.utils.RestClientUtils.createBasicClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.cs.spinnaker.jobmanager.JobManager;
import uk.ac.manchester.cs.spinnaker.model.Icinga2CheckResult;
import uk.ac.manchester.cs.spinnaker.rest.Icinga2;

public class Icinga2StatusMonitorManager implements StatusMonitorManager {

    private static final long TIMEOUT = 60;

    private static final int STATUS = 0;

    private static final String STATUS_MESSAGE = "OK";

    private static final int STATUS_UPDATE_TTL_MULTIPLIER = 2;

    private Icinga2 icinga;

    @Value("${icinga2.url}")
    private String icingaUrl;

    @Value("${icinga2.host:@null}")
    private String host;

    @Value("${icinca2.service:@null}")
    private String service;

    @Value("${icinga2.username}")
    private String username;

    @Value("${icinga2.password}")
    private String password;

    /**
     * Initialise the service.
     * @throws MalformedURLException if the URL isn't a URL
     */
    @PostConstruct
    private void init() throws MalformedURLException {
        final ResteasyClient client = new ResteasyClientBuilder().
                connectTimeout(TIMEOUT, TimeUnit.SECONDS).
                readTimeout(TIMEOUT, TimeUnit.SECONDS).build();
        icinga = createBasicClient(new URL(icingaUrl), username, password, Icinga2.class);
        icinga = client.target(icingaUrl).proxy(Icinga2.class);
    }


    @Override
    public void updateStatus(int runningJobs, int nBoardsInUse) {
        String performanceData = runningJobs + " running jobs\n"
                + nBoardsInUse + " boards in use";
        int ttl = JobManager.STATUS_UPDATE_PERIOD * STATUS_UPDATE_TTL_MULTIPLIER;
        Icinga2CheckResult result = new Icinga2CheckResult(
                STATUS, STATUS_MESSAGE, performanceData, ttl, host, service);
        icinga.processCheckResult(result);
    }

}
