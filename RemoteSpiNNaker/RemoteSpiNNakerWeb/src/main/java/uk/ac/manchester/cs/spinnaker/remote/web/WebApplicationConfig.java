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

import static java.lang.System.getProperty;
import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * Main web-app entry point. Launches the rest of the application and replaces
 * web.xml.
 */
public class WebApplicationConfig implements WebApplicationInitializer {
    /**
     * The name of the <i>system property</i> that describes where to load
     * configuration properties from.
     */
    public static final String LOCATION_PROPERTY =
            "remotespinnaker.properties.location";
    /**
     * The name of the filter chain.
     */
    private static final String FILTER_NAME = "springSecurityFilterChain";

    /**
     * Whether to add the filter or not.
     */
    private static final boolean ADD_FILTER = false;

    /**
     * Whether to add the servlet or not.
     */
    private static final boolean ADD_SERVLET = true;

    @Override
    public void onStartup(final ServletContext container)
            throws ServletException {
        try {
            final var properties = getPropertySource();
            if (ADD_SERVLET | ADD_FILTER) {
                container.addListener(getContextLoaderListener(properties));
            }
            if (ADD_SERVLET) {
                addServlet(container, properties);
            }
            if (ADD_FILTER) {
                addFilterChain(container);
            }
        } catch (final IOException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Get the context load listener.
     * @param properties The properties of the listener
     * @return The listener
     */
    private ContextLoaderListener
            getContextLoaderListener(final PropertySource<?> properties) {
        final var annotationConfig =
                new AnnotationConfigWebApplicationContext();
        annotationConfig.getEnvironment().getPropertySources()
                .addFirst(properties);
        annotationConfig.register(RemoteSpinnakerBeans.class);
        return new ContextLoaderListener(annotationConfig);
    }

    /**
     * Add a servlet.
     * @param container The servlet context
     * @param properties The properties of the servlet
     */
    private void addServlet(final ServletContext container,
            final PropertySource<?> properties) {
        container.addServlet("cxf", CXFServlet.class)
                .addMapping(properties.getProperty("cxf.path") + "/*");
    }

    /**
     * Add a filter chain.
     * @param container The context of the chain.
     */
    private void addFilterChain(final ServletContext container) {
        container.addFilter(FILTER_NAME, new DelegatingFilterProxy(FILTER_NAME))
                .addMappingForUrlPatterns(EnumSet.of(REQUEST, ERROR, ASYNC),
                        false, "/*");
    }

    /**
     * Get the source of the properties.
     * @return The property source
     * @throws IOException If something goes wrong
     */
    private PropertySource<?> getPropertySource() throws IOException {
        final var source = new File(getProperty(LOCATION_PROPERTY));
        return new ResourcePropertySource(source.toURI().toString());
    }
}
