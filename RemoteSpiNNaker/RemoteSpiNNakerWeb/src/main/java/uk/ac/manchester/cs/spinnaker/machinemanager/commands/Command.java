package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialisable request to spalloc.
 *
 * @param <A>
 *            The type of arguments.
 */
public abstract class Command<A> {

    /**
     * The name of the command.
     */
    private final String command;

    /**
     * The arguments to the command.
     */
    private final List<A> args = new ArrayList<>();

    /**
     * The KW Args to be passed to the command.
     */
    private final Map<String, Object> kwargs = new HashMap<>();

    /**
     * Add to the keyword arguments part.
     *
     * @param key
     *            The keyword
     * @param value
     *            The argument value; will be converted to a string
     */
    protected final void addKwArg(final String key, final Object value) {
        kwargs.put(key, value);
    }

    /**
     * Add to the positional arguments part.
     *
     * @param values
     *            The arguments to add.
     */
    @SafeVarargs
    protected final void addArg(final A... values) {
        for (final A value : values) {
            args.add(value);
        }
    }

    /**
     * Create a command.
     *
     * @param commandParam
     *            The command token.
     */
    public Command(final String commandParam) {
        this.command = commandParam;
    }

    /**
     * Get the command name.
     *
     * @return The command name
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get the command arguments.
     *
     * @return The command arguments.
     */
    public List<A> getArgs() {
        return args;
    }

    /**
     * Get the command keyword arguments.
     *
     * @return The command keyword arguments
     */
    public Map<String, Object> getKwargs() {
        return kwargs;
    }
}
