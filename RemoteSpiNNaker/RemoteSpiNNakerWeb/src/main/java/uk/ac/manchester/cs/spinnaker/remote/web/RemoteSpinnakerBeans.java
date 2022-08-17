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
package uk.ac.manchester.cs.spinnaker.remote.web;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.net.URL;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.converter.Converter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.cs.spinnaker.jobmanager.JobExecuterFactory;
import uk.ac.manchester.cs.spinnaker.jobmanager.JobManager;
import uk.ac.manchester.cs.spinnaker.jobmanager.LocalJobExecuterFactory;
import uk.ac.manchester.cs.spinnaker.jobmanager.XenVMExecuterFactory;
import uk.ac.manchester.cs.spinnaker.machine.SpinnakerMachine;
import uk.ac.manchester.cs.spinnaker.machinemanager.FixedMachineManagerImpl;
import uk.ac.manchester.cs.spinnaker.machinemanager.MachineManager;
import uk.ac.manchester.cs.spinnaker.machinemanager.SpallocMachineManagerImpl;
import uk.ac.manchester.cs.spinnaker.nmpi.NMPIQueueManager;
import uk.ac.manchester.cs.spinnaker.output.OutputManagerImpl;
import uk.ac.manchester.cs.spinnaker.rest.OutputManager;
import uk.ac.manchester.cs.spinnaker.rest.utils.NullExceptionMapper;
import uk.ac.manchester.cs.spinnaker.status.Icinga2StatusMonitorManagerImpl;
import uk.ac.manchester.cs.spinnaker.status.NullStatusMonitorManagerImpl;
import uk.ac.manchester.cs.spinnaker.status.StatusCakeStatusMonitorManagerImpl;
import uk.ac.manchester.cs.spinnaker.status.StatusMonitorManager;

/**
 * Builds the Spring beans in the application.
 */
@Configuration
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
public class RemoteSpinnakerBeans {

    /**
     * Types of status possible.
     */
    public enum StatusServiceType {
        /**
         * Status Cake service.
         */
        STATUS_CAKE,
        /**
         * Icigna2 service.
         */
        ICINGA2
    }

    /**
     * Configures using properties.
     *
     * @return bean
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer
            propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Parsing of Spinnaker machine descriptions.
     *
     * @return bean
     */
    @Bean
    public static ConversionServiceFactoryBean conversionService() {
        final ConversionServiceFactoryBean factory =
                new ConversionServiceFactoryBean();
        factory.setConverters(singleton((Converter<String, SpinnakerMachine>)
                SpinnakerMachine::parse));
        return factory;
    }

    /**
     * The context of the application.
     */
    @Autowired
    private ApplicationContext ctx;

    /**
     * Determine if machines are to be spalloc allocated.
     */
    @Value("${spalloc.enabled}")
    private boolean useSpalloc;

    /**
     * Determine if local jobs or Xen VMs are to be used.
     */
    @Value("${xen.server.enabled}")
    private boolean useXenVms;

    /**
     * The URL of the server.
     */
    @Value("${baseserver.url}${cxf.path}${cxf.rest.path}/")
    private URL baseServerUrl;

    /**
     * The URL of the server for local (non-external) access.
     */
    @Value("${localbaseserver.url}${cxf.path}${cxf.rest.path}/")
    private URL localBaseServerUrl;

    /**
     * The REST path of the server.
     */
    @Value("${cxf.rest.path}")
    private String restPath;

    /**
     * The OIDC redirect URL to return to when authenticated.
     */
    @Value("${baseserver.url}${callback.path}")
    private String oidcRedirectUri;

    /**
     * Determine whether status updates should be done.
     */
    @Value("${status.update}")
    private boolean updateStatus;

    /**
     * The type of status service to use.
     */
    @Value("${status.update.type}")
    private StatusServiceType statusType;

    /**
     * The machine manager; direct or via spalloc.
     *
     * @return bean
     */
    @Bean
    public MachineManager machineManager() {
        if (useSpalloc) {
            return new SpallocMachineManagerImpl();
        }
        return new FixedMachineManagerImpl();
    }

    /**
     * The queue manager.
     *
     * @return bean
     */
    @Bean
    public NMPIQueueManager queueManager() {
        return new NMPIQueueManager();
    }

    /**
     * The executer factory; local or inside Xen VMs.
     *
     * @return bean
     */
    @Bean
    public JobExecuterFactory jobExecuterFactory() {
        if (!useXenVms) {
            return new LocalJobExecuterFactory();
        }
        return new XenVMExecuterFactory();
    }

    /**
     * The output manager.
     *
     * @return bean
     */
    @Bean
    public OutputManager outputManager() {
        // Pass this, as it is non-trivial constructed value
        return new OutputManagerImpl(baseServerUrl);
    }

    /**
     * The job manager.
     *
     * @return bean
     */
    @Bean
    public JobManager jobManager() {
        // Pass this, as it is non-trivial constructed value
        return new JobManager(localBaseServerUrl);
    }

    /**
     * The status monitor manager.
     *
     * @return bean
     */
    @Bean
    public StatusMonitorManager statusMonitorManager() {
        if (updateStatus) {
            if (statusType == StatusServiceType.STATUS_CAKE) {
                return new StatusCakeStatusMonitorManagerImpl();
            } else if (statusType == StatusServiceType.ICINGA2) {
                return new Icinga2StatusMonitorManagerImpl();
            } else {
                throw new RuntimeException(
                        "Unknown status service type: " + statusType);
            }
        }
        return new NullStatusMonitorManagerImpl();
    }

    /**
     * The JAX-RS interface.
     *
     * @return bean
     */
    @Bean
    public Server jaxRsServer() {
        final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(restPath);
        factory.setBus(ctx.getBean(SpringBus.class));
        factory.setServiceBeans(asList(outputManager(), jobManager()));
        factory.setProviders(
                asList(new JacksonJsonProvider(), new NullExceptionMapper()));
        return factory.create();
    }
}
