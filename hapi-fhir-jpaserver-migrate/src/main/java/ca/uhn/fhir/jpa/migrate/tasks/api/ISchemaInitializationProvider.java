package ca.uhn.fhir.jpa.migrate.tasks.api;

/*-
 * #%L
 * HAPI FHIR JPA Server - Migration
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

import ca.uhn.fhir.jpa.migrate.DriverTypeEnum;
import ca.uhn.fhir.jpa.migrate.tasks.SchemaInitializationProvider;

import java.util.List;

public interface ISchemaInitializationProvider {

	List<String> getSqlStatements(DriverTypeEnum theDriverType);

	String getSchemaExistsIndicatorTable();

    String getSchemaDescription();

	SchemaInitializationProvider setSchemaDescription(String theSchemaDescription);
}
