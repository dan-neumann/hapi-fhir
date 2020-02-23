package org.hl7.fhir.common.hapi.validation;

import ca.uhn.fhir.context.support.IContextValidationSupport;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@SuppressWarnings("unchecked")
public class CachingValidationSupport extends BaseValidationSupportWrapper implements IContextValidationSupport {

	private static final Logger ourLog = LoggerFactory.getLogger(CachingValidationSupport.class);
	private final Cache<String, Object> myCache;
	private final Cache<String, Object> myValidateCodeCache;
	private final Cache<String, Object> myLookupCodeCache;


	public CachingValidationSupport(IContextValidationSupport theWrap) {
		super(theWrap.getFhirContext(), theWrap);
		myValidateCodeCache = Caffeine
			.newBuilder()
			.expireAfterWrite(60, TimeUnit.SECONDS)
			.maximumSize(5000)
			.build();
		myLookupCodeCache = Caffeine
			.newBuilder()
			.expireAfterWrite(60, TimeUnit.SECONDS)
			.maximumSize(5000)
			.build();
		myCache = Caffeine
			.newBuilder()
			.expireAfterWrite(60, TimeUnit.SECONDS)
			.maximumSize(5000)
			.build();
	}

	@Override
	public List<IBaseResource> fetchAllConformanceResources() {
		String key = "fetchAllConformanceResources";
		return loadFromCache(myCache, key, t -> super.fetchAllConformanceResources());
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllStructureDefinitions() {
		String key = "fetchAllStructureDefinitions";
		return loadFromCache(myCache, key, t -> super.fetchAllStructureDefinitions());
	}

	@Override
	public <T extends IBaseResource> T fetchResource(Class<T> theClass, String theUri) {
		return loadFromCache(myCache, "fetchResource " + theClass.getName() + " " + theUri,
			t -> super.fetchResource(theClass, theUri));
	}

	@Override
	public boolean isCodeSystemSupported(String theSystem) {
		String key = "isCodeSystemSupported " + theSystem;
		Boolean retVal = loadFromCache(myCache, key, t -> super.isCodeSystemSupported(theSystem));
		assert retVal != null;
		return retVal;
	}

	@Override
	public CodeValidationResult validateCode(IContextValidationSupport theRootValidationSupport, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
		String key = "validateCode " + theCodeSystem + " " + theCode + " " + defaultIfBlank(theValueSetUrl, "NO_VS");
		return loadFromCache(myValidateCodeCache, key, t -> super.validateCode(theRootValidationSupport, theCodeSystem, theCode, theDisplay, theValueSetUrl));
	}

	@Override
	public LookupCodeResult lookupCode(IContextValidationSupport theRootValidationSupport, String theSystem, String theCode) {
		String key = "lookupCode " + theSystem + " " + theCode;
		return loadFromCache(myLookupCodeCache, key, t -> super.lookupCode(theRootValidationSupport, theSystem, theCode));
	}

	@SuppressWarnings("OptionalAssignedToNull")
	@Nullable
	private <T> T loadFromCache(Cache theCache, String theKey, Function<String, T> theLoader) {
		// FIXME: remove
		ourLog.info("Fetching from cache: {}", theKey);

		Function<String, Optional<T>> loaderWrapper = key -> Optional.ofNullable(theLoader.apply(theKey));
		Optional<T> result = (Optional<T>) theCache.get(theKey, loaderWrapper);
		assert result != null;

		// FIXME: remove
		ourLog.info("Done fetching from cache: {}", theKey);

		return result.orElse(null);

	}

	public void flushCaches() {
		myCache.invalidateAll();
	}
}
