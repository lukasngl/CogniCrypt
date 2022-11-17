package crypto.cryslhandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import crypto.rules.CrySLException;
import crypto.rules.CrySLExceptionConstraint;
import crypto.rules.CrySLMethod;
import de.darmstadt.tu.crossing.crySL.ExceptionAggregate;
import de.darmstadt.tu.crossing.crySL.ExceptionDeclaration;
import de.darmstadt.tu.crossing.crySL.Method;
import de.darmstadt.tu.crossing.crySL.Object;
import de.darmstadt.tu.crossing.crySL.ObjectDecl;
import de.darmstadt.tu.crossing.crySL.Par;
import de.darmstadt.tu.crossing.crySL.RequiredBlock;
import de.darmstadt.tu.crossing.crySL.SuperType;
import de.darmstadt.tu.crossing.crySL.Exception;

/**
 * Helper class to derive {@link CrySLExceptionConstraint}'s from the events.
 */
public abstract class ExceptionsReader {
	public static List<CrySLExceptionConstraint> getExceptionConstraints(RequiredBlock eventsBlock) {
		return eventsBlock.getReq_event().stream()
				.filter(event -> event instanceof SuperType)
				.map(event -> (SuperType) event)
				.flatMap(meth -> ExceptionsReader.resolveExceptionsStream(meth.getException())
						.map(exception -> {
							CrySLMethod method = ExceptionsReader.toCrySLMethod(meth.getMeth());
							return new CrySLExceptionConstraint(method, exception);
						}))
				.collect(Collectors.toList());
	}

	public static CrySLMethod toCrySLMethod(final Method method) {
		String name = method.getMethName().getQualifiedName();
		List<Par> pars = method.getParList() == null ? Collections.emptyList() : method.getParList().getParameters();
		List<Entry<String, String>> parameters = pars.stream()
				.map(parameter -> parameter.getVal() == null
						? new SimpleEntry<>("_", "void")
						: resolveObject((parameter.getVal())))
				.collect(Collectors.toList());
		return new CrySLMethod(name, parameters, null, resolveObject(method.getLeftSide()));
	}


	public static Entry<String, String> resolveObject(final Object o) {
		if (o == null)
			return new SimpleEntry<>("_", "void");
		else
			return new SimpleEntry<>(o.getName(), ((ObjectDecl)o.eContainer()).getObjectType().getQualifiedName());
	}

	public static Collection<CrySLException> resolveExceptions(final Exception exception) {
		return resolveExceptionsStream(exception).collect(Collectors.toList());
	}

	public static Stream<CrySLException> resolveExceptionsStream(final Exception exception) {
		if (exception instanceof ExceptionDeclaration)
			return resolveExceptionsStream((ExceptionDeclaration) exception);
		if (exception instanceof ExceptionAggregate)
			return resolveExceptionsStream((ExceptionAggregate) exception);
		return Stream.empty();
	}

	protected static Stream<CrySLException> resolveExceptionsStream(final ExceptionAggregate exception) {
		return exception.getExceptions().stream()
				.flatMap(ExceptionsReader::resolveExceptionsStream);
	}

	protected static Stream<CrySLException> resolveExceptionsStream(final ExceptionDeclaration exception) {
		return Stream.of(toCrySLException(exception));
	}

	public static CrySLException toCrySLException(final ExceptionDeclaration exception) {
		return new CrySLException(exception.getException().getIdentifier());
	}
}


