package ca.uhn.fhir.cql.config;

/*-
 * #%L
 * HAPI FHIR - Clinical Quality Language
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.cql.common.provider.CqlProviderFactory;
import ca.uhn.fhir.cql.common.provider.EvaluationProviderFactory;
import ca.uhn.fhir.cql.common.provider.LibraryResolutionProvider;
import ca.uhn.fhir.cql.r4.provider.LibraryResolutionProviderImpl;
import ca.uhn.fhir.cql.r4.evaluation.ProviderFactory;
import ca.uhn.fhir.cql.r4.helper.LibraryHelper;
import ca.uhn.fhir.cql.r4.provider.JpaTerminologyProvider;
import ca.uhn.fhir.cql.r4.provider.MeasureOperationsProvider;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.rp.r4.ValueSetResourceProvider;
import ca.uhn.fhir.jpa.term.api.ITermReadSvcR4;

import org.opencds.cqf.cql.engine.model.ModelResolver;

import java.util.Map;

import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CqlR4Config extends BaseCqlConfig {

	@Lazy
	@Bean
	CqlProviderFactory cqlProviderFactory() {
		return new CqlProviderFactory();
	}

	@Lazy
	@Bean
	TerminologyProvider terminologyProvider(ITermReadSvcR4 theITermReadSvc, ValueSetResourceProvider theValueSetResourceProvider, IValidationSupport theValidationSupport) {
		return new JpaTerminologyProvider(theITermReadSvc,theValueSetResourceProvider, theValidationSupport);
	}

	@Lazy
	@Bean
	EvaluationProviderFactory evaluationProviderFactory(FhirContext theFhirContext, DaoRegistry theDaoRegistry, TerminologyProvider theLocalSystemTerminologyProvider, ModelResolver modelResolver) {
		return new ProviderFactory(theFhirContext, theDaoRegistry, theLocalSystemTerminologyProvider, modelResolver);
	}

	@Lazy
	@Bean
	LibraryResolutionProvider libraryResolutionProvider() {
		return new LibraryResolutionProviderImpl();
	}

	@Lazy
	@Bean
	public MeasureOperationsProvider measureOperationsProvider() {
		return new MeasureOperationsProvider();
	}

	@Bean
	public ModelResolver fhirModelResolver () {
		return new CachingModelResolverDecorator(new R4FhirModelResolver());
	}

	@Bean
	public LibraryHelper libraryHelper(Map<VersionedIdentifier, Model> globalModelCache) {
		return new LibraryHelper(globalModelCache);
	}
}
