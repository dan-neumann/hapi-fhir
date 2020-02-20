package ca.uhn.fhir.jpa.subscription.module.config;

/*-
 * #%L
 * HAPI FHIR Subscription Server
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
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
import ca.uhn.fhir.context.support.IContextValidationSupport;
import ca.uhn.fhir.jpa.searchparam.config.SearchParamR4Config;
import org.hl7.fhir.common.hapi.validation.DefaultProfileValidationSupport;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import({SearchParamR4Config.class})
public class SubscriptionR4Config extends BaseSubscriptionConfig {

	@Primary
	@Bean(autowire = Autowire.BY_NAME, name = "myJpaValidationSupportChainR4")
	public IContextValidationSupport validationSupportChainR4() {
		return new DefaultProfileValidationSupport(FhirContext.forR4());
	}
}
