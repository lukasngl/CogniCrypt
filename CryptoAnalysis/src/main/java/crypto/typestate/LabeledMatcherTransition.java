package crypto.typestate;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import crypto.rules.CrySLMethod;
import soot.SootClass;
import soot.SootMethod;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.State;

public class LabeledMatcherTransition extends MatcherTransition {

	public static LabeledMatcherTransition getErrorTransition(
			final State from,
			final Collection<SootMethod> matchingMethods,
			final Parameter param,
			final State to,
			final Type type) {
		final List<CrySLMethod> label = Collections.emptyList();
		return new LabeledMatcherTransition(from, matchingMethods, label, param, to, type, true);
	}

	public static LabeledMatcherTransition getNormalTranstion(
			final State from,
			final List<CrySLMethod> label,
			final Parameter param,
			final State to,
			final Type type) {
		final Collection<SootMethod> matchingMethods = CrySLMethodToSootMethod.v().convert(label);
		return new LabeledMatcherTransition(from, matchingMethods, label, param, to, type, false);
	}

	/**
	 * Match the called method against a declared method and checker whether
	 * the called method could actually be the declared one.
	 */
	public static boolean matches(final SootMethod called, final SootMethod declared) {
		// Name is equal
		if (!called.getName().equals(declared.getName()))
			return false;
		// declaring class is or is super-interface/super-class of actual class
		if (!isSubtype(called.getDeclaringClass(), declared.getDeclaringClass()))
			return false;
		// Number of Parameters are equal
		if (!(called.getParameterCount() == declared.getParameterCount()))
			return false;
		// Parameters are equal
		if (!called.getParameterTypes().equals(declared.getParameterTypes()))
			return false;
		// nice, declared is the declared version of called
		return true;
	}

	/**
	 * Returns whether parent is a super-type of child, i.e. if they
	 * are the same, child implements or extends parent transitively.
	 */
	public static boolean isSubtype(final SootClass child, final SootClass parent) {
		try {
			return Class.forName(parent.getName())
					.isAssignableFrom(Class.forName(child.getName()));
		} catch (final Throwable e) {
			return false;
		}
	}

	private final boolean isErrorTransition;

	private final List<CrySLMethod> label;

	private final Collection<SootMethod> matchingMethods;

	protected LabeledMatcherTransition(
			final State from,
			final Collection<SootMethod> matchingMethods,
			final List<CrySLMethod> label,
			final Parameter param,
			final State to,
			final Type type,
			final boolean isErrorTransition) {
		super(from, matchingMethods, param, to, type);
		this.matchingMethods = matchingMethods;
		this.label = label;
		this.isErrorTransition = isErrorTransition;
	}

	public boolean isErrorTransition() {
		return this.isErrorTransition;
	}

	public boolean isNormalTransition() {
		return !this.isErrorTransition;
	}

	/**
	 * The matches method of {@link MatcherTransition} matches Methods taken
	 * from some {@link soot.jimple.InvokeExpr}'s.
	 * The method getDeclaringClass() will return the object's class they are
	 * called on not the actual declaring class.
	 *
	 * Thus, if the class under spec does not declare the method,
	 * {@link CrySLMethodToSootMethod} won't find a matching method with the
	 * same declaring class and the label will not contain the method.
	 *
	 * We therefore check if there is a matching Method if the
	 * {@link MatcherTransition} returns false.
	 *
	 * The state machine is per Class, so every method will have the same
	 * declaring class and it is correct to return true if it matches the
	 * method of *some* super-type.
	 *
	 * @see typestate.finiteautomata.MatcherTransition#matches(soot.SootMethod)
	 */
	@Override
	public boolean matches(final SootMethod method) {
		if (super.matches(method))
			return true;

		for (final SootMethod m : this.matchingMethods) {
			if (matches(method, m))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return super.toString() + " (FUZZY)";
	}

	public List<CrySLMethod> label() {
		return label;
	}
}
