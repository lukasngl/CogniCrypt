package crypto.constraints;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import boomerang.jimple.Statement;
import crypto.analysis.AlternativeReqPredicate;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.ClassSpecification;
import crypto.analysis.CrySLResultsReporter;
import crypto.analysis.RequiredCrySLPredicate;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ImpreciseValueExtractionError;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ICrySLPredicateParameter;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLConstraint;
import crypto.rules.CrySLPredicate;
import soot.Type;

public class ConstraintSolver {

	public final static List<String> predefinedPreds = Arrays.asList("callTo", "noCallTo", "neverTypeOf", "length",
			"notHardCoded", "instanceOf");
	private final Set<ISLConstraint> relConstraints = Sets.newHashSet();
	private final List<ISLConstraint> requiredPredicates = Lists.newArrayList();
	private final Collection<Statement> collectedCalls;
	private final CrySLResultsReporter reporter;
	private final AnalysisSeedWithSpecification object;

	public ConstraintSolver(AnalysisSeedWithSpecification object, Collection<Statement> collectedCalls,
			CrySLResultsReporter crySLResultsReporter) {
		this.object = object;
		this.collectedCalls = collectedCalls;
		this.reporter = crySLResultsReporter;
		partitionConstraints();
	}

	public Multimap<CallSiteWithParamIndex, Type> getPropagatedTypes() {
		return this.object.getParameterAnalysis().getPropagatedTypes();
	}

	public Collection<CallSiteWithParamIndex> getParameterAnalysisQuerySites() {
		return this.object.getParameterAnalysis().getAllQuerySites();
	}

	public ClassSpecification getClassSpec() {
		return this.object.getSpec();
	}

	public Collection<Statement> getCollectedCalls() {
		return collectedCalls;
	}

	public CrySLResultsReporter getReporter() {
		return reporter;
	}

	public AnalysisSeedWithSpecification getObject() {
		return object;
	}

	public Multimap<CallSiteWithParamIndex, ExtractedValue> getParsAndVals() {
		return this.object.getParameterAnalysis().getCollectedValues();
	}

	/**
	 * @return the allConstraints
	 */
	public List<ISLConstraint> getAllConstraints() {
		return this.getClassSpec().getRule().getConstraints();
	}

	/**
	 * @return the relConstraints
	 */
	public Set<ISLConstraint> getRelConstraints() {
		return relConstraints;
	}

	public List<ISLConstraint> getRequiredPredicates() {
		return requiredPredicates;
	}

	public int evaluateRelConstraints() {
		int fail = 0;
		for (ISLConstraint con : getRelConstraints()) {
			EvaluableConstraint currentConstraint = EvaluableConstraint.getInstance(con, this);
			currentConstraint.evaluate();
			for (AbstractError e : currentConstraint.getErrors()) {
				if (e instanceof ImpreciseValueExtractionError) {
					getReporter().reportError(getObject(),
							new ImpreciseValueExtractionError(con, e.getErrorLocation(), e.getRule()));
					break;
				} else {
					fail++;
					getReporter().reportError(getObject(), e);
				}
			}
		}
		return fail;
	}

	/**
	 * (Probably) partitions Cosntraints into required Predicates and "normal"
	 * constraints (relConstraints).
	 */
	private void partitionConstraints() {
		for (ISLConstraint cons : getAllConstraints()) {

			Set<String> involvedVarNames = cons.getInvolvedVarNames();
			for (CallSiteWithParamIndex cwpi : this.getParameterAnalysisQuerySites()) {
				involvedVarNames.remove(cwpi.getVarName());
			}

			if (involvedVarNames.isEmpty() || (cons.toString().contains("speccedKey") && involvedVarNames.size() == 1)) {
				if (cons instanceof CrySLPredicate) {
					RequiredCrySLPredicate pred = retrieveValuesForPred(cons);
					if (pred != null) {
						CrySLPredicate innerPred = pred.getPred();
						if (innerPred != null) {
							relConstraints.add(innerPred);
							requiredPredicates.add(pred);
						}
					}
				} else if (cons instanceof CrySLConstraint) {
					ISLConstraint right = ((CrySLConstraint) cons).getRight();
					if (right instanceof CrySLPredicate && !predefinedPreds.contains(((CrySLPredicate) right).getPredName())) {
						requiredPredicates.add(collectAlternativePredicates((CrySLConstraint) cons, null));
					} else {
						relConstraints.add(cons);
					}
				} else {
					relConstraints.add(cons);
				}
			}
		}
	}

	private ISLConstraint collectAlternativePredicates(CrySLConstraint cons, AlternativeReqPredicate alt) {
		CrySLPredicate right = (CrySLPredicate) cons.getRight();
		if (alt == null) {
			alt = new AlternativeReqPredicate(right, right.getLocation());
		} else {
			alt.addAlternative(right);
		}

		if (cons.getLeft() instanceof CrySLPredicate) {
			alt.addAlternative((CrySLPredicate) cons.getLeft());
		} else {
			return collectAlternativePredicates((CrySLConstraint) cons.getLeft(), alt);
		}

		return alt;
	}

	private RequiredCrySLPredicate retrieveValuesForPred(ISLConstraint cons) {
		CrySLPredicate pred = (CrySLPredicate) cons;
		for (CallSiteWithParamIndex cwpi : this.getParameterAnalysisQuerySites()) {
			for (ICrySLPredicateParameter p : pred.getParameters()) {
				// TODO: FIX Cipher rule
				if (p.getName().equals("transformation"))
					continue;
				if (cwpi.getVarName().equals(p.getName())) {
					return new RequiredCrySLPredicate(pred, cwpi.stmt());
				}
			}
		}
		return null;
	}
}
