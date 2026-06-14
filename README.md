package healthrashi.service;

import healthrashi.data.entity.AbdmTransaction;
import healthrashi.data.entity.AbhaAccount;
import healthrashi.data.entity.ConsentRecord;
import healthrashi.model.response.ProfileDto;
import healthrashi.repository.AbdmTransactionRepository;
import healthrashi.repository.AbhaAccountRepository;
import healthrashi.repository.ConsentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AccountStore {

    private static final Logger log = LoggerFactory.getLogger(AccountStore.class);

    private final AbhaAccountRepository abhaAccountRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final AbdmTransactionRepository abdmTransactionRepository;

    public AccountStore(
            AbhaAccountRepository abhaAccountRepository,
            ConsentRecordRepository consentRecordRepository,
            AbdmTransactionRepository abdmTransactionRepository
    ) {
        this.abhaAccountRepository = abhaAccountRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.abdmTransactionRepository = abdmTransactionRepository;
    }

    // ─── Upsert account ───────────────────────────────────────────────────────

    public void upsertAccount(ProfileDto p, String source) {

        if (p.getAbhaNumber() == null || p.getAbhaNumber().isBlank()) return;

        AbhaAccount acc = abhaAccountRepository.findByAbhaNumber(p.getAbhaNumber()).orElse(null);

        if (acc == null) {
            acc = new AbhaAccount();
            acc.setAbhaNumber(p.getAbhaNumber());
            acc.setSource(source);
            acc.setCreatedAtUtc(LocalDateTime.now());
        }

        if (p.getAbhaAddress() != null) acc.setAbhaAddress(p.getAbhaAddress());
        if (p.getFullName() != null) acc.setFullName(p.getFullName());
        if (p.getGender() != null) acc.setGender(p.getGender());
        if (p.getDateOfBirth() != null) acc.setDateOfBirth(p.getDateOfBirth());
        if (p.getMobile() != null) acc.setMobile(p.getMobile());
        if (p.getEmail() != null) acc.setEmail(p.getEmail());
        if (p.getPhoto() != null) acc.setKycPhoto(p.getPhoto());

        acc.setUpdatedAtUtc(LocalDateTime.now());
        abhaAccountRepository.save(acc);
    }

    /**
     * FIX: Services call upsertAccountAsync() — alias added here.
     */
    public void upsertAccountAsync(ProfileDto p, String source) {   // ← FIX: added
        upsertAccount(p, source);
    }

    // ─── Update address ───────────────────────────────────────────────────────

    public void updateAddress(String abhaNumber, String abhaAddress) {

        Optional<AbhaAccount> accOpt = abhaAccountRepository.findByAbhaNumber(abhaNumber);
        if (accOpt.isEmpty()) return;

        AbhaAccount acc = accOpt.get();
        acc.setAbhaAddress(abhaAddress);
        acc.setUpdatedAtUtc(LocalDateTime.now());
        abhaAccountRepository.save(acc);
    }

    /**
     * FIX: Services call updateAddressAsync().
     */
    public void updateAddressAsync(String abhaNumber, String abhaAddress) { // ← FIX: added
        updateAddress(abhaNumber, abhaAddress);
    }

    // ─── Record consent ───────────────────────────────────────────────────────

    /**
     * Full 7-arg version used by internal AccountStore logic.
     */
    public void recordConsent(String flow, boolean accepted, String consentCode,
                              String consentVersion, String txnId, String abhaNumber, String consentText) {
        ConsentRecord record = new ConsentRecord();
        record.setFlow(flow);
        record.setAccepted(accepted);
        record.setConsentCode(consentCode);
        record.setConsentVersion(consentVersion);
        record.setTxnId(txnId);
        record.setAbhaNumber(abhaNumber);
        record.setConsentText(consentText);
        record.setAcceptedAtUtc(LocalDateTime.now());
        consentRecordRepository.save(record);
    }

    /**
     * FIX: EnrollmentService calls recordConsentAsync(flow, accepted, code, version) — 4 args.
     */
    public void recordConsentAsync(String flow, boolean accepted,    // ← FIX: added
                                   String consentCode, String consentVersion) {
        recordConsent(flow, accepted, consentCode, consentVersion, null, null, null);
    }

    // ─── Log / audit ──────────────────────────────────────────────────────────

    /**
     * Full 7-arg log writer.
     */
    public void log(String flow, String step, String status, String txnId,
                    String masked, String errorCode, String message) {
        try {
            AbdmTransaction tx = new AbdmTransaction();
            tx.setFlow(flow);
            tx.setStep(step);
            tx.setStatus(status);
            tx.setTxnId(txnId);
            tx.setMaskedIdentifier(masked);
            tx.setErrorCode(errorCode);
            tx.setMessage(message);
            tx.setCreatedAtUtc(LocalDateTime.now());
            abdmTransactionRepository.save(tx);
        } catch (Exception ex) {
            log.warn("Failed to write audit log entry ({}/{}).", flow, step, ex);
        }
    }

    // FIX: All overloads below added — callers use different arities of logAsync()

    /** logAsync(flow, step, status, txnId, masked, abhaNumber) — 6 args */
    public void logAsync(String flow, String step, String status, String txnId,
                         String masked, String abhaNumber) {         // ← FIX
        log(flow, step, status, txnId, masked, null, abhaNumber);
    }

    /** logAsync(flow, step, status, txnId, masked) — 5 args */
    public void logAsync(String flow, String step, String status,    // ← FIX
                         String txnId, String masked) {
        log(flow, step, status, txnId, masked, null, null);
    }

    /** logAsync(flow, step, status, txnId) — 4 args */
    public void logAsync(String flow, String step, String status, String txnId) { // ← FIX
        log(flow, step, status, txnId, null, null, null);
    }

    /** logAsync(flow, step, status, txnId, null, null, message) — 7 args (some null) */
    public void logAsync(String flow, String step, String status, String txnId,
                         String masked, String errorCode, String message) { // ← FIX
        log(flow, step, status, txnId, masked, errorCode, message);
    }
}
package healthrashi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import healthrashi.abdm.*;
import healthrashi.common.AbdmApiException;
import healthrashi.common.InputValidator;
import healthrashi.model.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ABHA verification / fetch (VRFY_ABHA_101–405).
 *
 * FIXES applied vs original:
 * - Removed all CompletableFuture — this project is synchronous.
 * - Replaced JsonUtil.createObject() with ObjectMapper (JsonUtil class doesn't exist).
 * - Replaced ProfileMapper.from() with ProfileDto.from() (ProfileMapper class doesn't exist).
 * - Fixed String passed as AbdmCallContext (selectAccountAsync).
 * - Added AADHAAR_SCOPE / MOBILE_SCOPE constants (referenced from controller).
 * - Fixed VerifiedProfileResponse / MultiAccountResponse constructors.
 * - Fixed AbdmApiException 3-arg call.
 * - Removed .exceptionally() called on ProfileDto (not a CompletableFuture).
 */
@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    // FIX: Controllers reference VerificationService.AADHAAR_SCOPE and MOBILE_SCOPE — added
    public static final String[] AADHAAR_SCOPE =
            new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.AADHAAR_VERIFY};   // ← FIX: added constant
    public static final String[] MOBILE_SCOPE =
            new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.MOBILE_VERIFY};    // ← FIX: added constant

    private final AbdmApiClient api;
    private final AbdmCryptoService crypto;
    private final ProfileService profileService;
    private final AccountStore store;

    // FIX: JsonUtil doesn't exist — use ObjectMapper directly
    private final ObjectMapper mapper = new ObjectMapper();                    // ← FIX

    private final Map<String, Object> cache = new HashMap<>();

    public VerificationService(AbdmApiClient api,
                                AbdmCryptoService crypto,
                                ProfileService profileService,
                                AccountStore store) {
        this.api = api;
        this.crypto = crypto;
        this.profileService = profileService;
        this.store = store;
    }

    // ─── Cache helpers ────────────────────────────────────────────────────────

    private void rememberVerifyPath(String txnId, String path) {
        cache.put("verifyPath:" + txnId, path);
    }

    private String verifyPathFor(String txnId) {
        Object v = cache.get("verifyPath:" + txnId);
        return (v instanceof String s && !s.isEmpty()) ? s : AbdmEndpoints.LOGIN_VERIFY;
    }

    private void rememberVerifyScope(String txnId, String[] scope) {
        cache.put("verifyScope:" + txnId, scope);
    }

    private String[] verifyScopeFor(String txnId) {
        Object v = cache.get("verifyScope:" + txnId);
        return (v instanceof String[]) ? (String[]) v : null;
    }

    // ─── Send OTP flows ───────────────────────────────────────────────────────

    /**
     * FIX: Controller calls verify.sendAadhaarOtp(value, false) — synchronous, returns TxnResponse.
     */
    public TxnResponse sendAadhaarOtp(String identifier, boolean isAbhaNumber) throws Exception {  // ← FIX: sync
        return sendLoginOtp(
                isAbhaNumber ? AbdmLoginHint.ABHA_NUMBER : AbdmLoginHint.AADHAAR,
                identifier,
                AbdmOtpSystem.AADHAAR,
                new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.AADHAAR_VERIFY}
        );
    }

    /**
     * FIX: Controller calls verify.sendAbhaMobileOtp(value) — sync.
     */
    public TxnResponse sendAbhaMobileOtp(String abhaId) throws Exception {  // ← FIX: sync
        if (isAbhaAddress(abhaId)) {
            return sendAbhaAddressOtp(abhaId);
        }
        return sendLoginOtp(
                AbdmLoginHint.ABHA_NUMBER,
                abhaId,
                AbdmOtpSystem.ABDM,
                new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.MOBILE_VERIFY}
        );
    }

    private TxnResponse sendAbhaAddressOtp(String abhaAddress) throws Exception {
        String addr = abhaAddress.trim();
        String[] scope = new String[]{AbdmScopes.ABHA_ADDRESS_LOGIN, AbdmScopes.MOBILE_VERIFY};

        ObjectNode body = mapper.createObjectNode();                          // ← FIX: was JsonUtil.createObject()
        body.put("loginHint", AbdmLoginHint.ABHA_ADDRESS);
        body.put("loginId", crypto.encrypt(addr));
        body.put("otpSystem", AbdmOtpSystem.ABDM);
        body.putPOJO("scope", scope);

        JsonNode resp = api.postAsync(AbdmEndpoints.PHR_WEB_ABHA_REQUEST_OTP, body);

        String txnId = resp.path("txnId").asText();
        rememberVerifyPath(txnId, AbdmEndpoints.PHR_WEB_ABHA_VERIFY);
        rememberVerifyScope(txnId, scope);

        store.logAsync("verification", "request-otp:abha-address", "success", txnId);
        return new TxnResponse(txnId, resp.path("message").asText());
    }

    /**
     * FIX: Controller calls verify.sendMobileSearchOtp(mobile) — sync.
     */
    public TxnResponse sendMobileSearchOtp(String mobile) throws Exception {  // ← FIX: sync
        return sendLoginOtp(
                AbdmLoginHint.MOBILE,
                mobile,
                AbdmOtpSystem.ABDM,
                new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.MOBILE_VERIFY}
        );
    }

    private TxnResponse sendLoginOtp(String loginHint, String identifier,
                                      String otpSystem, String[] scope) throws Exception {
        String clean = switch (loginHint) {
            case AbdmLoginHint.AADHAAR -> InputValidator.validateAadhaar(identifier);
            case AbdmLoginHint.MOBILE -> InputValidator.validateMobile(identifier);
            case AbdmLoginHint.ABHA_NUMBER -> VerificationService.formatAbhaNumber(identifier);
            default -> identifier.replace(" ", "").trim();
        };

        ObjectNode body = mapper.createObjectNode();                          // ← FIX: was JsonUtil.createObject()
        body.put("loginHint", loginHint);
        body.put("loginId", crypto.encrypt(clean));
        body.put("otpSystem", otpSystem);
        body.putPOJO("scope", scope);

        JsonNode resp = api.postAsync(AbdmEndpoints.LOGIN_REQUEST_OTP, body);

        String txnId = resp.path("txnId").asText();
        store.logAsync("verification", "request-otp:" + loginHint, "success", txnId);
        return new TxnResponse(txnId, resp.path("message").asText());
    }

    // ─── Verify OTP flows ─────────────────────────────────────────────────────

    /**
     * FIX: Controller calls verify.verify(txnId, otp, scope) — sync, returns VerifiedProfileResponse.
     */
    public VerifiedProfileResponse verify(String txnId, String otp, String[] scope) throws Exception { // ← FIX: sync

        JsonNode json = doVerify(txnId, otp, scope);

        String token = EnrollmentService.extractToken(json);

        log.info("verify response keys: {}", json.fieldNames().toString());

        // FIX: was ProfileMapper.from() — ProfileMapper doesn't exist, use ProfileDto.from()
        ProfileDto profileDto = ProfileDto.from(json.path("ABHAProfile")); // ← FIX

        if (profileDto.getAbhaNumber() == null || profileDto.getAbhaNumber().isEmpty()) {
            profileDto = merge(profileDto, ProfileDto.fromJwt(token));
        }

        // FIX: was profile.getProfileAsync(token).exceptionally(...) — not a CompletableFuture
        ProfileDto full = null;
        try {
            full = profileService.getProfileAsync(token);                     // ← FIX: sync call
        } catch (Exception ex) {
            log.warn("profile fetch failed: {}", ex.getMessage());
        }

        if (full != null && (full.getAbhaNumber() != null || full.getFullName() != null)) {
            merge(full, profileDto);
            profileDto = full;
        }

        store.upsertAccountAsync(profileDto, "verification");
        store.logAsync("verification", "verify", "success", txnId, profileDto.getAbhaNumber());

        return new VerifiedProfileResponse(txnId, token, profileDto);         // ← FIX: 3-arg constructor
    }

    /**
     * FIX: Controller calls verify.verifyMobile(txnId, otp) — sync.
     */
    public MultiAccountResponse verifyMobile(String txnId, String otp) throws Exception { // ← FIX: sync

        String[] scope = new String[]{AbdmScopes.ABHA_LOGIN, AbdmScopes.MOBILE_VERIFY};
        JsonNode json = doVerify(txnId, otp, scope);

        List<AccountSummaryDto> accounts = new ArrayList<>();

        JsonNode arr = json.path("accounts");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                AccountSummaryDto dto = new AccountSummaryDto();
                dto.setAbhaNumber(n.path("abhaNumber").asText());
                dto.setName(n.path("name").asText());
                dto.setAbhaAddress(n.path("abhaAddress").asText());
                dto.setGender(n.path("gender").asText());
                dto.setDateOfBirth(n.path("dateOfBirth").asText());
                accounts.add(dto);
            }
        }

        store.logAsync("verification", "verify-mobile", "success",
                txnId, null, null, accounts.size() + " accounts");

        return new MultiAccountResponse(txnId, EnrollmentService.extractToken(json), accounts); // ← FIX: 3-arg
    }

    /**
     * FIX: Controller calls verify.selectAccount(txnId, sessionToken, abhaNumber, abhaAddress) — sync.
     * Original passed sessionToken (String) as AbdmCallContext — fixed.
     */
    public VerifiedProfileResponse selectAccount(String txnId, String sessionToken,  // ← FIX: sync
                                                  String abhaNumber, String abhaAddress) throws Exception {
        String hyphen = formatAbhaNumber(abhaNumber);

        List<Map<String, Object>> bodies = Arrays.asList(
                Map.of("txnId", txnId, "ABHANumber", hyphen),
                Map.of("txnId", txnId, "ABHANumber", abhaNumber.replaceAll("\\D", ""))
        );

        for (Map<String, Object> body : bodies) {
            try {
                // FIX: was api.post(path, body, finalToken) where finalToken is String.
                // Added overload in AbdmApiClient that accepts String xToken.
                AbdmResponse resp = api.post(AbdmEndpoints.LOGIN_VERIFY_USER, body, sessionToken); // ← FIX

                // FIX: was ProfileMapper.from() — doesn't exist
                ProfileDto profileDto = ProfileDto.from(resp.getJson().path("ABHAProfile")); // ← FIX

                store.upsertAccountAsync(profileDto, "verification");
                store.logAsync("verification", "verify-user", "success", txnId, profileDto.getAbhaNumber());

                return new VerifiedProfileResponse(txnId, sessionToken, profileDto); // ← FIX: 3-arg constructor

            } catch (Exception ignored) {
                log.warn("verify/user attempt failed for {}", body);
            }
        }

        // FIX: was new AbdmApiException(502, "verify-user", "msg") — 3-arg, now supported
        throw new AbdmApiException(502, "verify-user", "All verification attempts failed");
    }

    // ─── Internal verify ──────────────────────────────────────────────────────

    private JsonNode doVerify(String txnId, String otp, String[] scope) throws Exception {

        String cleanOtp = InputValidator.validateOtp(otp);
        String[] effectiveScope = verifyScopeFor(txnId);
        if (effectiveScope == null) effectiveScope = scope;

        ObjectNode body = mapper.createObjectNode();                          // ← FIX: was JsonUtil.createObject()
        body.putPOJO("scope", effectiveScope);

        ObjectNode auth = mapper.createObjectNode();
        auth.put("txnId", txnId);
        auth.put("otpValue", crypto.encrypt(cleanOtp));
        auth.put("timeStamp", AbdmSessionService.iso8601Now());
        body.set("authData", auth);

        String path = verifyPathFor(txnId);

        return api.postAsync(path, body);                                     // ← FIX: sync ObjectNode overload
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public static String formatAbhaNumber(String input) {
        String s = input == null ? "" : input.trim();
        String digits = s.replaceAll("\\D", "");
        if (digits.length() == 14) {
            return digits.substring(0, 2) + "-" +
                    digits.substring(2, 6) + "-" +
                    digits.substring(6, 10) + "-" +
                    digits.substring(10);
        }
        return s;
    }

    private static ProfileDto merge(ProfileDto primary, ProfileDto fallback) {
        if (fallback == null) return primary;
        if (primary.getAbhaNumber() == null) primary.setAbhaNumber(fallback.getAbhaNumber());
        if (primary.getAbhaAddress() == null) primary.setAbhaAddress(fallback.getAbhaAddress());
        if (primary.getFullName() == null) primary.setFullName(fallback.getFullName());
        if (primary.getGender() == null) primary.setGender(fallback.getGender());
        if (primary.getDateOfBirth() == null) primary.setDateOfBirth(fallback.getDateOfBirth());
        if (primary.getMobile() == null) primary.setMobile(fallback.getMobile());
        if (primary.getEmail() == null) primary.setEmail(fallback.getEmail());
        return primary;
    }

    private boolean isAbhaAddress(String id) {
        return id != null && (id.contains("@") || id.matches(".*[a-zA-Z].*"));
    }
}
package healthrashi.model.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Base64;

@Data
public class ProfileDto {

    private String abhaNumber;
    private String abhaAddress;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String dateOfBirth;
    private String mobile;
    private String email;
    private String photo;
    private Boolean isNew;

    // ─── Static factory methods ───────────────────────────────────────────────

    /**
     * FIX: EnrollmentService and ProfileService call ProfileDto.from(JsonNode).
     * This was completely missing — added here.
     * Maps all common ABDM field name variants into ProfileDto.
     */
    public static ProfileDto from(JsonNode node) {      // ← FIX: added
        ProfileDto dto = new ProfileDto();
        if (node == null || node.isMissingNode() || node.isNull()) return dto;

        dto.setAbhaNumber(firstText(node, "ABHANumber", "abhaNumber", "healthIdNumber"));
        dto.setAbhaAddress(firstText(node, "preferredAbhaAddress", "abhaAddress", "healthId", "phrAddress"));
        dto.setFullName(firstText(node, "fullName", "name", "patientName"));
        dto.setFirstName(firstText(node, "firstName"));
        dto.setMiddleName(firstText(node, "middleName"));
        dto.setLastName(firstText(node, "lastName"));
        dto.setGender(firstText(node, "gender"));
        dto.setDateOfBirth(firstText(node, "dateOfBirth", "dob"));
        dto.setMobile(firstText(node, "mobile", "mobileNumber", "phoneNumber"));
        dto.setEmail(firstText(node, "email", "emailId"));
        dto.setPhoto(firstText(node, "kycPhoto", "photo", "profilePhoto", "picture"));

        if (node.has("new")) {
            dto.setIsNew(node.get("new").asBoolean());
        } else if (node.has("isNew")) {
            dto.setIsNew(node.get("isNew").asBoolean());
        }
        return dto;
    }

    /**
     * FIX: VerificationService calls ProfileDto.fromJwt(token).
     * Decodes the JWT payload (base64) and maps fields into ProfileDto.
     * This was completely missing — added here.
     */
    public static ProfileDto fromJwt(String jwtToken) {    // ← FIX: added
        ProfileDto dto = new ProfileDto();
        if (jwtToken == null || jwtToken.isBlank()) return dto;

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) return dto;

            // JWT payload is the second part, base64url encoded
            String payloadBase64 = parts[1];
            // Pad base64 if necessary
            int pad = payloadBase64.length() % 4;
            if (pad == 2) payloadBase64 += "==";
            else if (pad == 3) payloadBase64 += "=";

            byte[] decoded = Base64.getUrlDecoder().decode(payloadBase64);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode node = mapper.readTree(json);

            dto.setAbhaNumber(firstText(node, "ABHANumber", "abhaNumber", "healthIdNumber", "sub"));
            dto.setAbhaAddress(firstText(node, "preferredAbhaAddress", "abhaAddress", "healthId"));
            dto.setFullName(firstText(node, "fullName", "name"));
            dto.setMobile(firstText(node, "mobile", "mobileNumber"));
            dto.setEmail(firstText(node, "email"));
            dto.setGender(firstText(node, "gender"));
            dto.setDateOfBirth(firstText(node, "dateOfBirth", "dob"));

        } catch (Exception ex) {
            // If JWT parsing fails, return empty DTO — caller handles null fields
        }
        return dto;
    }

    // ─── Private helper ───────────────────────────────────────────────────────

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode val = node.get(key);
            if (val != null && !val.isNull() && !val.asText("").isBlank()) {
                return val.asText();
            }
        }
        return null;
    }
}
package healthrashi.abdm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import healthrashi.config.AbdmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class AbdmApiClient {

    private final RestTemplate restTemplate;
    private final AbdmProperties abdmProperties;
    private final AbdmSessionService sessionService;
    private final ObjectMapper objectMapper;
    private String benefitName;

    private static final Logger log = LoggerFactory.getLogger(AbdmApiClient.class);

    public AbdmApiClient(
            RestTemplate restTemplate,
            AbdmProperties abdmProperties,
            AbdmSessionService sessionService,
            ObjectMapper objectMapper) {

        this.restTemplate = restTemplate;
        this.abdmProperties = abdmProperties;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    // ─── Synchronous core methods ──────────────────────────────────────────────

    public AbdmResponse post(String path, Object body, AbdmCallContext context) {
        return send(HttpMethod.POST, path, body, context);
    }

    public String getBenefitName() {
        return benefitName;
    }
    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }
    /**
     *
     * Overload used in VerificationService.selectAccountAsync —
     * token string passed directly instead of a context object.
     */
    public AbdmResponse post(String path, Object body, String xToken) {  // ← FIX: added overload
        AbdmCallContext ctx = AbdmCallContext.withToken(xToken);
        return send(HttpMethod.POST, path, body, ctx);
    }

    public AbdmResponse get(String path, AbdmCallContext context) {
        return send(HttpMethod.GET, path, null, context);
    }

    // ─── "Async" aliases ──────────────────────────────────────────────────────
    // The service layer calls these. Since this project is fully synchronous
    // (no CompletableFuture) these simply delegate to the sync versions and
    // return the JsonNode directly — matching how the callers use the return value.

    /**
     * Called as: api.postAsync(path, mapBody, null)
     * Matches EnrollmentService / DiagnosticsController usage.
     */
    public JsonNode postAsync(String path, Map<String, Object> body, AbdmCallContext context) { // ← FIX: added
        return post(path, body, context).getJson();
    }

    /**
     * Called as: api.postAsync(path, objectNode)
     * Matches VerificationService usage with ObjectNode bodies.
     */
    public JsonNode postAsync(String path, ObjectNode body) {  // ← FIX: added
        return post(path, body, AbdmCallContext.none()).getJson();
    }

    /**
     * Called as: api.postAsync(path, body, ctx) where ctx is AbdmCallContext.
     */
    public JsonNode postAsync(String path, Object body, Object context) {  // ← FIX: added generic overload
        AbdmCallContext ctx = (context instanceof AbdmCallContext)
                ? (AbdmCallContext) context
                : AbdmCallContext.none();
        return post(path, body, ctx).getJson();
    }

    /**
     * Called as: api.getAsync(path, context)
     */
    public JsonNode getAsync(String path, AbdmCallContext context) {  // ← FIX: added
        return get(path, context).getJson();
    }

    // ─── Core HTTP send ───────────────────────────────────────────────────────

    public AbdmResponse send(HttpMethod method, String path, Object body, AbdmCallContext context) {

        if (context == null) {
            context = AbdmCallContext.none();
        }

        String url = path.startsWith("http")
                ? path
                : abdmProperties.getAbhaApiBaseUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.add(AbdmHeaders.REQUEST_ID, UUID.randomUUID().toString());
        headers.add(AbdmHeaders.TIMESTAMP, AbdmSessionService.iso8601Now());
        headers.add(AbdmHeaders.AUTHORIZATION, "Bearer " + sessionService.getAccessToken());

        if (context.getXToken() != null) {
            headers.add(AbdmHeaders.X_TOKEN, "Bearer " + context.getXToken());
        }

        if (context.getTToken() != null) {
            String tokenValue = context.isTTokenNoBearer()
                    ? context.getTToken()
                    : "Bearer " + context.getTToken();
            headers.add(AbdmHeaders.T_TOKEN, tokenValue);
        }

        if (context.getTransactionId() != null) {
            headers.add(AbdmHeaders.TRANSACTION_ID, context.getTransactionId());
        }

        if (context.isUseBenefitName()
                && context.getBenefitName() != null
                && !context.getBenefitName().isBlank()) {
            headers.add(AbdmHeaders.BENEFIT_NAME, context.getBenefitName());
        }

        HttpEntity<?> entity = body != null
                ? new HttpEntity<>(body, headers)
                : new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(url, method, entity, byte[].class);

        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : null;

        byte[] responseBytes = response.getBody();

        if (contentType != null &&
                (contentType.startsWith("image/") || contentType.equals("application/octet-stream"))) {

            AbdmResponse result = new AbdmResponse();
            result.setStatusCode(response.getStatusCode().value());
            result.setBytes(responseBytes);
            result.setContentType(contentType);
            return result;
        }

        String rawBody = responseBytes != null
                ? new String(responseBytes, StandardCharsets.UTF_8)
                : "";

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("ABDM {} {} -> {} : {}", method, path, response.getStatusCode().value(), rawBody);
            throw AbdmErrorParser.toException(
                    response.getStatusCode().value(),
                    rawBody,
                    "ABDM call failed: " + method + " " + path
            );
        }

        JsonNode json = null;
        if (!rawBody.isBlank()) {
            try {
                json = objectMapper.readTree(rawBody);
            } catch (Exception ignored) {
            }
        }

        AbdmResponse result = new AbdmResponse();
        result.setStatusCode(response.getStatusCode().value());
        result.setRawBody(rawBody);
        result.setJson(json);
        result.setContentType(contentType);
        return result;
    }
}package healthrashi.config;

/**
 * Strongly-typed ABDM configuration class (Spring Boot equivalent of C# options pattern).
 *
 * This maps to "Abdm" section in application.yml / application.properties.
 * Secrets (clientId / clientSecret) must be provided via environment variables or secure config.
 */
public class AbdmProperties {

    /**
     * Configuration section name (used for binding reference if needed).
     */
    public static final String SECTION_NAME = "Abdm";

    /**
     * X-CM-ID value:
     * "sbx" for sandbox environment, "abdm" for production.
     */
    private String environment = "sbx";

    /**
     * Gateway base URL (used for session token generation).
     * Example: https://dev.abdm.gov.in/api/hiecm
     */
    private String gatewayBaseUrl = "https://dev.abdm.gov.in/api/hiecm";

    /**
     * ABHA API base URL.
     * Example: https://abhasbx.abdm.gov.in/abha/api
     */
    private String abhaApiBaseUrl = "https://abhasbx.abdm.gov.in/abha/api";

    /**
     * PHR web base URL (used for login/verify flows).
     * Example: https://abhasbx.abdm.gov.in/abha/api/v3/phr/web
     */
    private String phrWebBaseUrl = "https://abhasbx.abdm.gov.in/abha/api/v3/phr/web";

    /**
     * Patient share URL (PHR profile share to HIP endpoint).
     */
    private String patientShareUrl = "https://dev.abdm.gov.in/api/hiecm/patient-share/v3/on-share";

    /**
     * Consent code used for ABHA enrollment policy.
     */
    private String consentCode = "abha-enrollment";

    /**
     * Consent version used for compliance tracking.
     */
    private String consentVersion = "1.4";

    /**
     * Optional benefit name header (government/program-specific).
     */
    private String benefitName;

    // ---------------- Secrets (must come from env or secret manager) ----------------

    /**
     * Client ID for ABDM authentication.
     */
    private String clientId = "";

    /**
     * Client secret for ABDM authentication.
     */
    private String clientSecret = "";

    // ---------------- Getters and Setters ----------------

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public String getAbhaApiBaseUrl() {
        return abhaApiBaseUrl;
    }

    public void setAbhaApiBaseUrl(String abhaApiBaseUrl) {
        this.abhaApiBaseUrl = abhaApiBaseUrl;
    }

    public String getPhrWebBaseUrl() {
        return phrWebBaseUrl;
    }

    public void setPhrWebBaseUrl(String phrWebBaseUrl) {
        this.phrWebBaseUrl = phrWebBaseUrl;
    }

    public String getPatientShareUrl() {
        return patientShareUrl;
    }

    public void setPatientShareUrl(String patientShareUrl) {
        this.patientShareUrl = patientShareUrl;
    }

    public String getConsentCode() {
        return consentCode;
    }

    public void setConsentCode(String consentCode) {
        this.consentCode = consentCode;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
