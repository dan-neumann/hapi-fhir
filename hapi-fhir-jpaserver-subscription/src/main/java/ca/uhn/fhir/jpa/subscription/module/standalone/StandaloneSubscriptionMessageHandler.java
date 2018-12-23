package ca.uhn.fhir.jpa.subscription.module.standalone;

/*-
 * #%L
 * HAPI FHIR Subscription Server
 * %%
 * Copyright (C) 2014 - 2018 University Health Network
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
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.jpa.subscription.module.ResourceModifiedMessage;
import ca.uhn.fhir.jpa.subscription.module.cache.SubscriptionRegistry;
import ca.uhn.fhir.jpa.subscription.module.subscriber.ResourceModifiedJsonMessage;
import ca.uhn.fhir.jpa.subscription.module.subscriber.SubscriptionMatchingSubscriber;
import ca.uhn.fhir.model.dstu2.valueset.ResourceTypeEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

@Service
public class StandaloneSubscriptionMessageHandler implements MessageHandler {
	private static final Logger ourLog = LoggerFactory.getLogger(StandaloneSubscriptionMessageHandler.class);

	@Autowired
	FhirContext myFhirContext;
	@Autowired
	SubscriptionMatchingSubscriber mySubscriptionMatchingSubscriber;
	@Autowired
	SubscriptionRegistry mySubscriptionRegistry;

	@Override
	public void handleMessage(Message<?> theMessage) throws MessagingException {
		if (!(theMessage instanceof ResourceModifiedJsonMessage)) {
			ourLog.warn("Unexpected message payload type: {}", theMessage);
			return;
		}
		updateSubscriptionRegistryAndPerformMatching(((ResourceModifiedJsonMessage) theMessage).getPayload());
	}

	public void updateSubscriptionRegistryAndPerformMatching(ResourceModifiedMessage theResourceModifiedMessage) {
		IBaseResource resource = theResourceModifiedMessage.getNewPayload(myFhirContext);
		RuntimeResourceDefinition resourceDef = myFhirContext.getResourceDefinition(resource);

		if (resourceDef.getName().equals(ResourceTypeEnum.SUBSCRIPTION.getCode())) {
			mySubscriptionRegistry.registerSubscriptionUnlessAlreadyRegistered(resource);
		}
		mySubscriptionMatchingSubscriber.matchActiveSubscriptionsAndDeliver(theResourceModifiedMessage);
	}
}
