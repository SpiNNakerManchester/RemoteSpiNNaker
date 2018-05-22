package uk.ac.manchester.cs.spinnaker.machinemanager.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialisable request to a (remote) machine manager.
 *
 * @param <A>
 *            The type of arguments.
 */
public abstract class Command<A> {
	private final String command;
	private final List<A> args = new ArrayList<>();
	private final Map<String, String> kwargs = new HashMap<>();

	/**
	 * Add to the keyword arguments part.
	 *
	 * @param key
	 *            The keyword
	 * @param value
	 *            The argument value; will be converted to a string
	 */
	protected final void addKwArg(String key, Object value) {
		kwargs.put(key, value.toString());
	}

	/**
	 * Add to the positional arguments part.
	 *
	 * @param values
	 *            The arguments to add.
	 */
	@SafeVarargs
	protected final void addArg(A... values) {
		for (A value : values) {
			args.add(value);
		}
	}

	/**
	 * Create a command.
	 *
	 * @param command
	 *            The command token.
	 */
	protected Command(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public List<A> getArgs() {
		return args;
	}

	public Map<String, String> getKwargs() {
		return kwargs;
	}
}
