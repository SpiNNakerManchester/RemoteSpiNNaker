package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command<A> {
    private final String command;
    private final List<A> args = new ArrayList<>();
    private final Map<String, Object> kwargs = new HashMap<>();

    protected final void addKwArg(final String key, final Object value) {
        kwargs.put(key, value);
    }

    @SafeVarargs
    protected final void addArg(final A... values) {
        for (final A value : values) {
            args.add(value);
        }
    }

    public Command(final String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public List<A> getArgs() {
        return args;
    }

    public Map<String, Object> getKwargs() {
        return kwargs;
    }
}
