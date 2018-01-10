package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A command to be sent to spalloc.
 *
 * @param <A> The command arguments type
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
    private final Map<String, String> kwargs = new HashMap<>();

    /**
     * Add a KW Args argument.
     *
     * @param key The keyword
     * @param value The value
     */
    protected final void addKwArg(final String key, final Object value) {
        kwargs.put(key, value.toString());
    }

    /**
     * Add an Arg argument.
     * @param values The values of the argument
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
     * @param commandParam The command name
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
    public Map<String, String> getKwargs() {
        return kwargs;
    }
}
