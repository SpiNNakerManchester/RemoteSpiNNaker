package uk.ac.manchester.cs.spinnaker.remote.web;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.springframework.security.authentication.ClientAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.nimbusds.oauth2.sdk.ParseException;

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

/**
 * Builds the Spring beans in the application.
 */
@Configuration
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
public class RemoteSpinnakerBeans {
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
        factory.setConverters(
                singleton(new Converter<String, SpinnakerMachine>() {
                    @Override
                    public SpinnakerMachine convert(final String value) {
                        return SpinnakerMachine.parse(value);
                    }
                }));
        return factory;
    }

    @Autowired
    private ApplicationContext ctx;

    @Value("${spalloc.enabled}")
    private boolean useSpalloc;

    @Value("${xen.server.enabled}")
    private boolean useXenVms;

    @Value("${baseserver.url}${cxf.path}${cxf.rest.path}/")
    private URL baseServerUrl;

    @Value("${cxf.rest.path}")
    private String restPath;

    @Value("${baseserver.url}${callback.path}")
    private String oidcRedirectUri;

    /**
     * Configuration that connects to external HBP services.
     */
    // TODO unused
    class HbpServices {
        /**
         * Set up configuration of authentication.
         *
         * @param auth
         *            The handle to the authentication system.
         */
        // @Autowired
        public void configureGlobal(final AuthenticationManagerBuilder auth)
                throws Exception {
            auth.authenticationProvider(clientProvider());
        }

        /**
         * The collabratory security service.
         *
         * @return bean
         */
        // @Bean
        public CollabSecurityService collabSecurityService()
                throws MalformedURLException {
            return new CollabSecurityService();
        }

        /**
         * The HBP basic authentication client.
         *
         * @return bean
         */
        // @Bean
        public Client<?, ?> hbpAuthenticationClient() {
            return new BasicOidcClient();
        }

        /**
         * The HBP bearer authentication client.
         *
         * @return bean
         */
        // @Bean
        public Client<?, ?> hbpBearerClient()
                throws ParseException, MalformedURLException, IOException {
            return new BearerOidcClient();
        }

        /**
         * The list of various authentication clients to try to use.
         *
         * @return bean
         */
        // @Bean
        public Clients clients()
                throws ParseException, MalformedURLException, IOException {
            return new Clients(oidcRedirectUri, hbpAuthenticationClient(),
                    hbpBearerClient());
        }

        /**
         * The authentication provider.
         *
         * @return bean
         */
        // @Bean
        public ClientAuthenticationProvider clientProvider()
                throws ParseException, MalformedURLException, IOException {
            final ClientAuthenticationProvider provider =
                    new ClientAuthenticationProvider();
            provider.setClients(clients());
            return provider;
        }
    }

    // TODO unused
    // @Configuration
    // @Order(100)
    // public static class HbpAuthentication extends
    // WebSecurityConfigurerAdapter {
    // @Value("${cxf.path}${cxf.rest.path}")
    // String restServicePath;
    // @Value("${callback.path}")
    // private String callbackPath;
    //
    // @Autowired
    // OidcClient hbpAuthenticationClient;
    // @Autowired
    // BearerOidcClient hbpBearerClient;
    // @Autowired
    // Clients clients;
    //
    // @Override
    // public void configure(WebSecurity web) throws Exception {
    // Path path = AnnotationUtils.findAnnotation(
    // JobManager.class, Path.class);
    // web.ignoring().antMatchers(restServicePath + path.value() + "/**");
    // }
    //
    // @Override
    // protected void configure(HttpSecurity http) throws Exception {
    // Path path = AnnotationUtils.findAnnotation(
    // OutputManager.class, Path.class);
    // http.addFilterBefore(
    // directAuthFilter(),
    // UsernamePasswordAuthenticationFilter.class)
    // .addFilterBefore(
    // callbackFilter(),
    // UsernamePasswordAuthenticationFilter.class)
    // .exceptionHandling().authenticationEntryPoint(
    // hbpAuthenticationEntryPoint()).and()
    // .authorizeRequests().antMatchers(
    // restServicePath + path.value() + "/**").authenticated()
    // .anyRequest().permitAll();
    // }
    //
    // @Bean
    // public ClientAuthenticationFilter callbackFilter() throws Exception {
    // ClientAuthenticationFilter filter =
    // new ClientAuthenticationFilter(callbackPath);
    // filter.setClients(clients);
    // filter.setAuthenticationManager(authenticationManagerBean());
    // return filter;
    // }
    //
    // @Bean
    // public OncePerRequestFilter directAuthFilter() throws Exception {
    // DirectClientAuthenticationFilter filter =
    // new DirectClientAuthenticationFilter(
    // authenticationManagerBean());
    // filter.setClient(hbpBearerClient);
    // return filter;
    // }
    //
    // @Bean
    // public ClientAuthenticationEntryPoint hbpAuthenticationEntryPoint() {
    // ClientAuthenticationEntryPoint entryPoint =
    // new ClientAuthenticationEntryPoint();
    // entryPoint.setClient(hbpAuthenticationClient);
    // return entryPoint;
    // }
    // }

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
        return new JobManager(baseServerUrl);
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
