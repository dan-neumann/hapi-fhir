package org.hl7.fhir.common.hapi.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IContextValidationSupport;
import ca.uhn.fhir.util.VersionIndependentConcept;
import org.hl7.fhir.convertors.conv10_50.ValueSet10_50;
import org.hl7.fhir.convertors.conv30_50.CodeSystem30_50;
import org.hl7.fhir.convertors.conv30_50.ValueSet30_50;
import org.hl7.fhir.convertors.conv40_50.CodeSystem40_50;
import org.hl7.fhir.convertors.conv40_50.ValueSet40_50;
import org.hl7.fhir.dstu2.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.utilities.ValidationOptions;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class BaseStaticResourceValidationSupport extends BaseValidationSupport implements IContextValidationSupport {

	/**
	 * Constructor
	 */
	protected BaseStaticResourceValidationSupport(FhirContext theFhirContext) {
		super(theFhirContext);
	}

	@Override
	public ValueSetExpansionOutcome expandValueSet(IContextValidationSupport theRootValidationSupport, IBaseResource theValueSetToExpand) {

		IBaseResource expansion;
		switch (myCtx.getVersion().getVersion()) {
			case DSTU2_HL7ORG: {
				expansion = expandValueSetDstu2(theRootValidationSupport, (org.hl7.fhir.dstu2.model.ValueSet) theValueSetToExpand);
				break;
			}
			case DSTU3: {
				expansion = expandValueSetDstu3(theRootValidationSupport, (org.hl7.fhir.dstu3.model.ValueSet) theValueSetToExpand);
				break;
			}
			case R4: {
				expansion = expandValueSetR4(theRootValidationSupport, (org.hl7.fhir.r4.model.ValueSet) theValueSetToExpand);
				break;
			}
			case R5: {
				expansion = expandValueSetR5(theRootValidationSupport, (org.hl7.fhir.r5.model.ValueSet) theValueSetToExpand);
				break;
			}
			case DSTU2:
			case DSTU2_1:
			default:
				throw new IllegalArgumentException("Can not handle version: " + myCtx.getVersion().getVersion());
		}

		if (expansion == null) {
			return null;
		}

		return new ValueSetExpansionOutcome(expansion, null);
	}

	@Override
	public CodeValidationResult validateCode(IContextValidationSupport theRootValidationSupport, ValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
		IBaseResource system = null;
		if (!theOptions.isGuessSystem() && isNotBlank(theCodeSystem)) {
			system = fetchCodeSystem(theCodeSystem);
			if (system == null) {
				return null;
			}
		}

		IBaseResource vs;
		if (isNotBlank(theValueSetUrl)) {
			vs = fetchValueSet(theValueSetUrl);
			if (vs == null) {
				return null;
			}
		} else {
			switch (myCtx.getVersion().getVersion()) {
				case DSTU2_HL7ORG:
					vs = new org.hl7.fhir.dstu2.model.ValueSet()
						.setCompose(new org.hl7.fhir.dstu2.model.ValueSet.ValueSetComposeComponent()
							.addInclude(new org.hl7.fhir.dstu2.model.ValueSet.ConceptSetComponent().setSystem(theCodeSystem)));
					break;
				case DSTU3:
					vs = new org.hl7.fhir.dstu3.model.ValueSet()
						.setCompose(new org.hl7.fhir.dstu3.model.ValueSet.ValueSetComposeComponent()
							.addInclude(new org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent().setSystem(theCodeSystem)));
					break;
				case R4:
					vs = new org.hl7.fhir.r4.model.ValueSet()
						.setCompose(new org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent()
							.addInclude(new org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent().setSystem(theCodeSystem)));
					break;
				case R5:
					vs = new org.hl7.fhir.r5.model.ValueSet()
						.setCompose(new org.hl7.fhir.r5.model.ValueSet.ValueSetComposeComponent()
							.addInclude(new org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent().setSystem(theCodeSystem)));
					break;
				case DSTU2:
				case DSTU2_1:
				default:
					throw new IllegalArgumentException("Can not handle version: " + myCtx.getVersion().getVersion());
			}
		}

		ValueSetExpansionOutcome valueSetExpansionOutcome = expandValueSet(theRootValidationSupport, vs);
		if (valueSetExpansionOutcome == null) {
			return null;
		}

		IBaseResource expansion = valueSetExpansionOutcome.getValueSet();
		boolean caseSensitive;

		List<VersionIndependentConcept> codes = new ArrayList<>();
		switch (myCtx.getVersion().getVersion()) {
			case DSTU2_HL7ORG: {
				org.hl7.fhir.dstu2.model.ValueSet expansionVs = (org.hl7.fhir.dstu2.model.ValueSet) expansion;
				List<org.hl7.fhir.dstu2.model.ValueSet.ValueSetExpansionContainsComponent> contains = expansionVs.getExpansion().getContains();
				flattenAndConvertCodesDstu2(contains, codes);
				caseSensitive = true;
				break;
			}
			case DSTU3: {
				org.hl7.fhir.dstu3.model.ValueSet expansionVs = (org.hl7.fhir.dstu3.model.ValueSet) expansion;
				List<org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent> contains = expansionVs.getExpansion().getContains();
				flattenAndConvertCodesDstu3(contains, codes);
				caseSensitive = system == null || ((org.hl7.fhir.dstu3.model.CodeSystem) system).getCaseSensitive();
				break;
			}
			case R4: {
				org.hl7.fhir.r4.model.ValueSet expansionVs = (org.hl7.fhir.r4.model.ValueSet) expansion;
				List<org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent> contains = expansionVs.getExpansion().getContains();
				flattenAndConvertCodesR4(contains, codes);
				caseSensitive = system == null || ((org.hl7.fhir.r4.model.CodeSystem) system).getCaseSensitive();
				break;
			}
			case R5: {
				org.hl7.fhir.r5.model.ValueSet expansionVs = (org.hl7.fhir.r5.model.ValueSet) expansion;
				List<org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent> contains = expansionVs.getExpansion().getContains();
				flattenAndConvertCodesR5(contains, codes);
				caseSensitive = system == null || ((org.hl7.fhir.r5.model.CodeSystem) system).getCaseSensitive();
				break;
			}
			case DSTU2:
			case DSTU2_1:
			default:
				throw new IllegalArgumentException("Can not handle version: " + myCtx.getVersion().getVersion());
		}

		for (VersionIndependentConcept nextExpansionCode : codes) {

			boolean codeMatches;
			if (caseSensitive) {
				codeMatches = theCode.equals(nextExpansionCode.getCode());
			} else {
				codeMatches = theCode.equalsIgnoreCase(nextExpansionCode.getCode());
			}
			if (codeMatches) {
				if (theOptions.isGuessSystem() || nextExpansionCode.getSystem().equals(theCodeSystem)) {
					return new CodeValidationResult()
						.setCode(theCode);
				}
			}
		}

		return new CodeValidationResult()
			.setSeverity(ValidationMessage.IssueSeverity.WARNING.toCode())
			.setMessage("Unknown code for '" + theCodeSystem + "#" + theCode + "'");

	}

	@Override
	public LookupCodeResult lookupCode(IContextValidationSupport theRootValidationSupport, String theSystem, String theCode) {
		return validateCode(theRootValidationSupport, new ValidationOptions(), theSystem, theCode, null, null).asLookupCodeResult(theSystem, theCode);
	}

	@Nullable
	private org.hl7.fhir.dstu2.model.ValueSet expandValueSetDstu2(IContextValidationSupport theRootValidationSupport, ValueSet theInput) {
		Function<String, org.hl7.fhir.r5.model.CodeSystem> codeSystemLoader = t -> {
			org.hl7.fhir.dstu2.model.ValueSet codeSystem = (org.hl7.fhir.dstu2.model.ValueSet) theRootValidationSupport.fetchCodeSystem(t);
			CodeSystem retVal = new CodeSystem();
			addCodes(codeSystem.getCodeSystem().getConcept(), retVal.getConcept());
			return retVal;
		};
		Function<String, org.hl7.fhir.r5.model.ValueSet> valueSetLoader = t -> {
			org.hl7.fhir.dstu2.model.ValueSet valueSet = (org.hl7.fhir.dstu2.model.ValueSet) theRootValidationSupport.fetchValueSet(t);
			return ValueSet10_50.convertValueSet(valueSet);
		};

		org.hl7.fhir.r5.model.ValueSet input = ValueSet10_50.convertValueSet(theInput);
		org.hl7.fhir.r5.model.ValueSet output = expandValueSetR5(input, codeSystemLoader, valueSetLoader);
		return ValueSet10_50.convertValueSet(output);
	}

	private void addCodes(List<ValueSet.ConceptDefinitionComponent> theSourceList, List<CodeSystem.ConceptDefinitionComponent> theTargetList) {
		for (ValueSet.ConceptDefinitionComponent nextSource : theSourceList) {
			CodeSystem.ConceptDefinitionComponent targetConcept = new CodeSystem.ConceptDefinitionComponent().setCode(nextSource.getCode()).setDisplay(nextSource.getDisplay());
			theTargetList.add(targetConcept);
			addCodes(nextSource.getConcept(), targetConcept.getConcept());
		}
	}

	@Nullable
	private org.hl7.fhir.dstu3.model.ValueSet expandValueSetDstu3(IContextValidationSupport theRootValidationSupport, org.hl7.fhir.dstu3.model.ValueSet theInput) {
		Function<String, org.hl7.fhir.r5.model.CodeSystem> codeSystemLoader = t -> {
			org.hl7.fhir.dstu3.model.CodeSystem codeSystem = (org.hl7.fhir.dstu3.model.CodeSystem) theRootValidationSupport.fetchCodeSystem(t);
			return CodeSystem30_50.convertCodeSystem(codeSystem);
		};
		Function<String, org.hl7.fhir.r5.model.ValueSet> valueSetLoader = t -> {
			org.hl7.fhir.dstu3.model.ValueSet valueSet = (org.hl7.fhir.dstu3.model.ValueSet) theRootValidationSupport.fetchValueSet(t);
			return ValueSet30_50.convertValueSet(valueSet);
		};

		org.hl7.fhir.r5.model.ValueSet input = ValueSet30_50.convertValueSet(theInput);
		org.hl7.fhir.r5.model.ValueSet output = expandValueSetR5(input, codeSystemLoader, valueSetLoader);
		return ValueSet30_50.convertValueSet(output);
	}

	@Nullable
	private org.hl7.fhir.r4.model.ValueSet expandValueSetR4(IContextValidationSupport theRootValidationSupport, org.hl7.fhir.r4.model.ValueSet theInput) {
		Function<String, org.hl7.fhir.r5.model.CodeSystem> codeSystemLoader = t -> {
			org.hl7.fhir.r4.model.CodeSystem codeSystem = (org.hl7.fhir.r4.model.CodeSystem) theRootValidationSupport.fetchCodeSystem(t);
			return CodeSystem40_50.convertCodeSystem(codeSystem);
		};
		Function<String, org.hl7.fhir.r5.model.ValueSet> valueSetLoader = t -> {
			org.hl7.fhir.r4.model.ValueSet valueSet = (org.hl7.fhir.r4.model.ValueSet) theRootValidationSupport.fetchValueSet(t);
			return ValueSet40_50.convertValueSet(valueSet);
		};

		org.hl7.fhir.r5.model.ValueSet input = ValueSet40_50.convertValueSet(theInput);
		org.hl7.fhir.r5.model.ValueSet output = expandValueSetR5(input, codeSystemLoader, valueSetLoader);
		return ValueSet40_50.convertValueSet(output);
	}

	@Nullable
	private org.hl7.fhir.r5.model.ValueSet expandValueSetR5(IContextValidationSupport theRootValidationSupport, org.hl7.fhir.r5.model.ValueSet theInput) {
		Function<String, org.hl7.fhir.r5.model.CodeSystem> codeSystemLoader = t -> (org.hl7.fhir.r5.model.CodeSystem) theRootValidationSupport.fetchCodeSystem(t);
		Function<String, org.hl7.fhir.r5.model.ValueSet> valueSetLoader = t -> (org.hl7.fhir.r5.model.ValueSet) theRootValidationSupport.fetchValueSet(t);

		return expandValueSetR5(theInput, codeSystemLoader, valueSetLoader);
	}

	@Nullable
	private org.hl7.fhir.r5.model.ValueSet expandValueSetR5(org.hl7.fhir.r5.model.ValueSet theInput, Function<String, CodeSystem> theCodeSystemLoader, Function<String, org.hl7.fhir.r5.model.ValueSet> theValueSetLoader) {
		Set<VersionIndependentConcept> concepts = new HashSet<>();

		try {
			expandValueSetR5IncludeOrExclude(concepts, theCodeSystemLoader, theValueSetLoader, theInput.getCompose().getInclude(), true);
			expandValueSetR5IncludeOrExclude(concepts, theCodeSystemLoader, theValueSetLoader, theInput.getCompose().getExclude(), false);
		} catch (ExpansionCouldNotBeCompletedInternallyException e) {
			return null;
		}

		org.hl7.fhir.r5.model.ValueSet retVal = new org.hl7.fhir.r5.model.ValueSet();
		for (VersionIndependentConcept next : concepts) {
			retVal.getExpansion().addContains().setSystem(next.getSystem()).setCode(next.getCode());
		}

		return retVal;
	}

	private void expandValueSetR5IncludeOrExclude(Set<VersionIndependentConcept> theConcepts, Function<String, CodeSystem> theCodeSystemLoader, Function<String, org.hl7.fhir.r5.model.ValueSet> theValueSetLoader, List<org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent> theComposeList, boolean theComposeListIsInclude) throws ExpansionCouldNotBeCompletedInternallyException {
		for (org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent nextInclude : theComposeList) {

			List<VersionIndependentConcept> nextCodeList = new ArrayList<>();
			String system = nextInclude.getSystem();
			if (isNotBlank(system)) {
				CodeSystem codeSystem = theCodeSystemLoader.apply(system);
				if (codeSystem == null) {
					throw new ExpansionCouldNotBeCompletedInternallyException();
				}
				if (codeSystem.getContent() == CodeSystem.CodeSystemContentMode.NOTPRESENT) {
					throw new ExpansionCouldNotBeCompletedInternallyException();
				}

				Set<String> wantCodes;
				if (nextInclude.getConcept().isEmpty()) {
					wantCodes = null;
				} else {
					wantCodes = nextInclude.getConcept().stream().map(t -> t.getCode()).collect(Collectors.toSet());
				}

				addCodes(system, codeSystem.getConcept(), nextCodeList, wantCodes);
			}

			for (CanonicalType nextValueSetInclude : nextInclude.getValueSet()) {
				org.hl7.fhir.r5.model.ValueSet vs = theValueSetLoader.apply(nextValueSetInclude.getValueAsString());
				if (vs != null) {
					org.hl7.fhir.r5.model.ValueSet subExpansion = expandValueSetR5(vs, theCodeSystemLoader, theValueSetLoader);
					if (subExpansion == null) {
						throw new ExpansionCouldNotBeCompletedInternallyException();
					}
					for (org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent next : subExpansion.getExpansion().getContains()) {
						nextCodeList.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
					}
				}
			}

			if (theComposeListIsInclude) {
				theConcepts.addAll(nextCodeList);
			} else {
				theConcepts.removeAll(nextCodeList);
			}

		}

	}

	private void addCodes(String theSystem, List<CodeSystem.ConceptDefinitionComponent> theSource, List<VersionIndependentConcept> theTarget, Set<String> theCodeFilter) {
		for (CodeSystem.ConceptDefinitionComponent next : theSource) {
			if (isNotBlank(next.getCode())) {
				if (theCodeFilter == null || theCodeFilter.contains(next.getCode())) {
					theTarget.add(new VersionIndependentConcept(theSystem, next.getCode()));
				}
			}
			addCodes(theSystem, next.getConcept(), theTarget, theCodeFilter);
		}
	}

	private static class ExpansionCouldNotBeCompletedInternallyException extends Exception {

	}

	private static void flattenAndConvertCodesDstu2(List<org.hl7.fhir.dstu2.model.ValueSet.ValueSetExpansionContainsComponent> theInput, List<VersionIndependentConcept> theVersionIndependentConcepts) {
		for (org.hl7.fhir.dstu2.model.ValueSet.ValueSetExpansionContainsComponent next : theInput) {
			theVersionIndependentConcepts.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
			flattenAndConvertCodesDstu2(next.getContains(), theVersionIndependentConcepts);
		}
	}

	private static void flattenAndConvertCodesDstu3(List<org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent> theInput, List<VersionIndependentConcept> theVersionIndependentConcepts) {
		for (org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent next : theInput) {
			theVersionIndependentConcepts.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
			flattenAndConvertCodesDstu3(next.getContains(), theVersionIndependentConcepts);
		}
	}

	private static void flattenAndConvertCodesR4(List<org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent> theInput, List<VersionIndependentConcept> theVersionIndependentConcepts) {
		for (org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent next : theInput) {
			theVersionIndependentConcepts.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
			flattenAndConvertCodesR4(next.getContains(), theVersionIndependentConcepts);
		}
	}

	private static void flattenAndConvertCodesR5(List<org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent> theInput, List<VersionIndependentConcept> theVersionIndependentConcepts) {
		for (org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent next : theInput) {
			theVersionIndependentConcepts.add(new VersionIndependentConcept(next.getSystem(), next.getCode()));
			flattenAndConvertCodesR5(next.getContains(), theVersionIndependentConcepts);
		}
	}

	@SuppressWarnings("unchecked")
	static <T extends IBaseResource> List<T> toList(Map<String, IBaseResource> theMap) {
		ArrayList<IBaseResource> retVal = new ArrayList<>(theMap.values());
		return (List<T>) Collections.unmodifiableList(retVal);
	}

}
