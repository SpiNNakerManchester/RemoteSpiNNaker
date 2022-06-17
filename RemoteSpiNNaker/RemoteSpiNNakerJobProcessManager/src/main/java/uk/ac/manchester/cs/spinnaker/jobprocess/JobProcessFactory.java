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
package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;

/**
 * A factory for creating {@link JobProcess} instances given a
 * {@link JobParameters} instance.
 */
public class JobProcessFactory {

    /**
     * The thread group of the factory.
     */
    private final ThreadGroup threadGroup;

    /**
     * Create a factory.
     *
     * @param threadGroupParam
     *            The thread group for the factory. All threads created by the
     *            factory will be within this group.
     */
    public JobProcessFactory(final ThreadGroup threadGroupParam) {
        this.threadGroup = threadGroupParam;
    }

    /**
     * Create a factory.
     *
     * @param threadGroupName
     *            The name of the thread group for the factory. All threads
     *            created by the factory will be within this group.
     */
    public JobProcessFactory(final String threadGroupName) {
        this(new ThreadGroup(threadGroupName));
    }

    /**
	 * A version of {@link java.util.function.Supplier Supplier} that builds a
	 * job process. Required to work around type inference rules.
	 *
	 * @param <P> The type of job parameters
	 * @author Donal Fellows
	 */
    @FunctionalInterface
    public interface ProcessSupplier<P extends JobParameters> {
		/**
		 * Make a new instance of a job process.
		 *
		 * @return The instance.
		 */
    	JobProcess<P> get();
    }

    /**
	 * A map between parameter types and process types. Note that the type is
	 * guaranteed by the {@link #addMapping(Class,ProcessSupplier)} method,
	 * which is the only place that this map should be modified.
	 */
	private final Map<Class<? extends JobParameters>,
			ProcessSupplier<? extends JobParameters>> typeMap = new HashMap<>();

    /**
     * Adds a new type mapping.
     *
     * @param <P>
     *            The type of parameters that this mapping will handle.
     * @param parameterType
     *            The job parameter type
     * @param processType
     *            The job process type
     */
    public <P extends JobParameters> void addMapping(
            final Class<P> parameterType,
            final ProcessSupplier<P> processType) {
        typeMap.put(parameterType, processType);
    }

    /**
     * Get the types of parameters supported.
     *
     * @return A collection of types of parameters
     */
    public Collection<Class<? extends JobParameters>> getParameterTypes() {
        return typeMap.keySet();
    }

    /**
     * Creates a {@link JobProcess} given a {@link JobParameters} instance.
     *
     * @param <P>
     *            The type of the job parameters.
     * @param parameters
     *            The parameters of the job
     * @return A JobProcess matching the parameters
     * @throws IllegalAccessException
     *             If there is an error creating the class
     * @throws InstantiationException
     *             If there is an error creating the class
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    public <P extends JobParameters> JobProcess<P>
            createProcess(final P parameters)
                    throws InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException,
                    NoSuchMethodException, SecurityException {
        /*
         * We know that this is of the correct type, because the addMapping
         * method will only allow the correct type mapping in
         */
        @SuppressWarnings("unchecked")
        final JobProcess<P> process =
        		(JobProcess<P>) typeMap.get(parameters.getClass()).get();

        // Magically set the thread group if there is one
        setField(process, "threadGroup", threadGroup);

        return process;
    }

    /**
     * Set a static field in an object.
     *
     * @param clazz The class of the object
     * @param fieldName The name of the field to set
     * @param value The value to set the field to
     */
    @SuppressWarnings("unused")
    private static void setField(final Class<?> clazz, final String fieldName,
            final Object value) {
        try {
            final Field threadGroupField = clazz.getDeclaredField(fieldName);
            threadGroupField.setAccessible(true);
            threadGroupField.set(null, value);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            // Treat any exception as just a simple refusal to set the field.
        }
    }

    /**
     * Set a field in an instance.
     *
     * @param instance The instance
     * @param fieldName The name of the field
     * @param value The value to set
     */
    private static void setField(final Object instance, final String fieldName,
            final Object value) {
        try {
            final Field threadGroupField =
                    instance.getClass().getDeclaredField(fieldName);
            threadGroupField.setAccessible(true);
            threadGroupField.set(instance, value);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            // Treat any exception as just a simple refusal to set the field.
        }
    }
}
