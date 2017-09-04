package uk.ac.manchester.cs.spinnaker.jobprocess;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.manchester.cs.spinnaker.job.JobParameters;

/**
 * A factory for creating {@link JobProcess} instances given a
 * {@link JobParameters} instance
 */
public class JobProcessFactory {
    private final ThreadGroup threadGroup;

    public JobProcessFactory(final ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    public JobProcessFactory(final String threadGroupName) {
        this(new ThreadGroup(threadGroupName));
    }

    /**
     * A map between parameter types and process types. Note that the type is
     * guaranteed by the {@link #addMapping(Class,Class)} method, which is the
     * only place that this map should be modified.
     */
    private final Map<Class<? extends JobParameters>, Class<? extends JobProcess<? extends JobParameters>>> typeMap = new HashMap<>();

    /**
     * Adds a new type mapping.
     *
     * @param parameterType
     *            The job parameter type
     * @param processType
     *            The job process type
     */
    public <P extends JobParameters> void addMapping(
            final Class<P> parameterType,
            final Class<? extends JobProcess<P>> processType) {
        typeMap.put(parameterType, processType);
    }

    public Collection<Class<? extends JobParameters>> getParameterTypes() {
        return typeMap.keySet();
    }

    /**
     * Creates a {@link JobProcess} given a {@link JobParameters} instance.
     *
     * @param parameters
     *            The parameters of the job
     * @return A JobProcess matching the parameters
     * @throws IllegalAccessException
     *             If there is an error creating the class
     * @throws InstantiationException
     *             If there is an error creating the class
     */
    public <P extends JobParameters> JobProcess<P> createProcess(
            final P parameters)
            throws InstantiationException, IllegalAccessException {
        /*
         * We know that this is of the correct type, because the addMapping
         * method will only allow the correct type mapping in
         */
        @SuppressWarnings("unchecked")
        final Class<JobProcess<P>> processType = (Class<JobProcess<P>>) typeMap
                .get(parameters.getClass());

        final JobProcess<P> process = processType.newInstance();

        // Magically set the thread group if there is one
        setField(process, "threadGroup", threadGroup);

        return process;
    }

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

    private static void setField(final Object instance, final String fieldName,
            final Object value) {
        try {
            final Field threadGroupField = instance.getClass()
                    .getDeclaredField(fieldName);
            threadGroupField.setAccessible(true);
            threadGroupField.set(instance, value);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            // Treat any exception as just a simple refusal to set the field.
        }
    }
}
