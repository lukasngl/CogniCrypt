package crypto.rules;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import static java.util.AbstractMap.SimpleEntry;

import crypto.interfaces.ICrySLPredicateParameter;

public class CrySLMethod implements Serializable, ICrySLPredicateParameter {

	public static final String VOID = "void";
	public static final String ANY_TYPE = "AnyType";
	public static final String NO_NAME = "_";

	private static final long serialVersionUID = 1L;
	private final String methodName;
	private final Entry<String, String> retObject;
	/**
	 * List of Parameters, where a Parameter is an {@link java.util.Map.Entry}
	 * of Name and Type, both as {@link String}.
	 */
	private final List<Entry<String, String>> parameters;

	public CrySLMethod(String methodName, List<Entry<String, String>> parameters, Entry<String, String> retObject) {
		this.methodName = methodName;
		this.parameters = parameters;
		this.retObject = retObject;
	}

	/**
	 * @return the FQ methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return the short methodName
	 */
	public String getShortMethodName() {
		return methodName.substring(methodName.lastIndexOf(".") + 1);
	}

	/**
	 * @return the parameters
	 */
	public List<Entry<String, String>> getParameters() {
		return parameters;
	}

	public Entry<String, String> getRetObject() {
		return retObject;
	}

	public String toString() {
		return getName();
	}

	@Override
	public String getName() {
		StringBuilder stmntBuilder = new StringBuilder();
		String returnValue = retObject.getKey();
		if (!"_".equals(returnValue)) {
			stmntBuilder.append(returnValue);
			stmntBuilder.append(" = ");
		}
		
		stmntBuilder.append(this.methodName);
		stmntBuilder.append("(");

		stmntBuilder.append(parameters.stream()
				.map(param -> String.format("%s %s", param.getValue(), param.getKey()))
				.collect(Collectors.joining(", ")));

		stmntBuilder.append(");");
		return stmntBuilder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CrySLMethod)) {
			return false;
		}
		CrySLMethod other = (CrySLMethod) obj;
		return this.getMethodName().equals(other.getMethodName()) && parameters.equals(other.parameters);
	}

}
