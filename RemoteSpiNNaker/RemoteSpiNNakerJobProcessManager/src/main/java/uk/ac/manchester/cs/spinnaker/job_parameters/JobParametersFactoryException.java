package uk.ac.manchester.cs.spinnaker.job_parameters;

/**
 * Indicates that whilst the job type was supported, there was an error
 * converting the job to parameters.
 */
public class JobParametersFactoryException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Create an exception with a message.
     *
     * @param message The message
     */
    public JobParametersFactoryException(final String message) {
        super(message);
    }

    /**
     * Create an exception with a message and cause.
     *
     * @param message The message
     * @param cause The cause
     */
    public JobParametersFactoryException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
