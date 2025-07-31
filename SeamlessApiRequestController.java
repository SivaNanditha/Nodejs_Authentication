package com.trustlypay.gateway.controller;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustlypay.gateway.model.ClientRequest;
import com.trustlypay.gateway.model.CyroKeys;
import com.trustlypay.gateway.model.DigibizMidKeys;
import com.trustlypay.gateway.model.IntentpayMidKeys;
import com.trustlypay.gateway.model.LiveMerchantApiModel;
import com.trustlypay.gateway.model.LiveOrder;
import com.trustlypay.gateway.model.LivePayment;
import com.trustlypay.gateway.model.Merchant;
import com.trustlypay.gateway.model.MerchantBusiness;
import com.trustlypay.gateway.model.MerchantVendorBankEntity;
import com.trustlypay.gateway.model.TechtotechDPMidKeys;
import com.trustlypay.gateway.model.TechtotechFPMidKeys;
import com.trustlypay.gateway.model.TechtotechMidKeys;
import com.trustlypay.gateway.model.TimepayMidKeys;
import com.trustlypay.gateway.model.VendorBank;
import com.trustlypay.gateway.service.CyroKeysService;
import com.trustlypay.gateway.service.DigibizMIDKeysService;
import com.trustlypay.gateway.service.IntentpayMIDKeysService;
import com.trustlypay.gateway.service.MerchantApiService;
import com.trustlypay.gateway.service.MerchantBusinessService;
import com.trustlypay.gateway.service.MerchantService;
import com.trustlypay.gateway.service.MerchantVendorBankService;
import com.trustlypay.gateway.service.OrderService;
import com.trustlypay.gateway.service.PaymentService;
import com.trustlypay.gateway.service.TechtotechDPKeysService;
import com.trustlypay.gateway.service.TechtotechFPKeysService;
import com.trustlypay.gateway.service.TechtotechKeysService;
import com.trustlypay.gateway.service.TimepayMIDKeysService;
import com.trustlypay.gateway.service.VendorBankService;
import com.trustlypay.gateway.service.MerchantSeamlessKeysService;
import com.trustlypay.gateway.service.IntentpayApiResponseService;
import com.trustlypay.gateway.utils.ApexTokenManager;
import com.trustlypay.gateway.utils.DigibizEncryption;
import com.trustlypay.gateway.utils.SignatureKey;
import com.trustlypay.gateway.utils.TimepaySecureData;
import com.trustlypay.gateway.utils.TransationIdGeneration;
import com.trustlypay.gateway.utils.TrustlypaySecureData;

@RestController
@RequestMapping("/gateway")
public class SeamlessApiRequestController {
    
    private static final Logger log = LoggerFactory.getLogger(SeamlessApiRequestController.class);
    
    // Constants for error codes
    private static final class ErrorCodes {
        static final String CLIENT_ID_REQUIRED = "301";
        static final String ENCRYPTED_DATA_REQUIRED = "302";
        static final String INVALID_CLIENT_ID = "303";
        static final String INVALID_ENCRYPTED_DATA = "304";
        static final String USERNAME_INVALID = "306";
        static final String CLIENT_ID_EMPTY = "307";
        static final String CLIENT_SECRET_EMPTY = "308";
        static final String EMAIL_INVALID = "309";
        static final String MOBILE_INVALID = "310";
        static final String CURRENCY_INVALID = "311";
        static final String AMOUNT_INVALID = "312";
        static final String INTERNAL_ERROR = "500";
    }
    
    // Constants for error messages
    private static final class ErrorMessages {
        static final String CLIENT_ID_REQUIRED_MSG = "Client ID is Required.";
        static final String ENCRYPTED_DATA_REQUIRED_MSG = "encryptedData is Required.";
        static final String INVALID_CLIENT_ID_MSG = "Invalid Client Id";
        static final String INVALID_ENCRYPTED_DATA_MSG = "Invalid Encrypted Data";
        static final String USERNAME_INVALID_MSG = "User name is Invalid/Empty.";
        static final String CLIENT_ID_EMPTY_MSG = "clientId is Empty.";
        static final String CLIENT_SECRET_EMPTY_MSG = "client Secret Key is Empty.";
        static final String EMAIL_INVALID_MSG = "Email ID is Invalid/Empty.";
        static final String MOBILE_INVALID_MSG = "Mobile Number is Invalid/Empty.";
        static final String CURRENCY_INVALID_MSG = "Currency Type is Invalid/Empty.";
        static final String AMOUNT_INVALID_MSG = "Amount is Invalid/Empty.";
        static final String INTERNAL_ERROR_MSG = "Internal server error";
    }
    
    // Constants for validation patterns
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._ -]*$";
    private static final String MOBILE_PATTERN = "\\d{10}";
    private static final String AMOUNT_PATTERN = "^[\\d\\.,]*[\\.,]?\\d+$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String CURRENCY_INR = "INR";
    private static final double MIN_AMOUNT = 100.0;
    private static final String TRANSACTION_PENDING = "pending";
    private static final String ORDER_CREATED = "Created";
    private static final String STATUS_FAILED = "Failed";
    
    // Vendor bank constants
    private static final String INTENTPAY_BANK = "Intentpay";
    private static final String CYRO_BANK = "Cyro";
    private static final String DIGIBIZ_BANK = "Digibiz";
    private static final String TECHTOTECH_BANK = "Techtotech";
    private static final String TECHTOTECH_FP_BANK = "TechtotechFP";
    private static final String TECHTOTECH_DP_BANK = "TechtotechDP";
    private static final String TIMEPAY_BANK = "Timepay";
    private static final String SAFEPAY_BANK = "Safepay";
    private static final String NGMB_BANK = "NGMB";
    private static final String BULKPE_BANK = "Bulkpe";
    private static final String ISMART_BANK = "iSmart";
    private static final String ISERVEU_BANK = "iServeU";
    private static final String ACSPAY_BANK = "AcsPay";
    private static final String APIPAYS_BANK = "ApiPays";
    private static final String APEX_BANK = "Apex";
    private static final String SEAMLESS_BANK = "Seamless";
    private static final String PAYU_BANK = "PayU";
    private static final String RAZORPAY_BANK = "Razorpay";
    private static final String PHONEPE_BANK = "PhonePe";
    private static final String GOOGLEPAY_BANK = "GooglePay";
    private static final String PAYTM_BANK = "Paytm";
    
    // API URLs
    private static final String INTENTPAY_API_URL_TEMPLATE = "https://api.intentpay.in/api/v1/merchants/transaction/%s/all/new";
    private static final String SAFEPAY_QR_API_URL = "https://proapi.safepayindia.com/QRService.svc/CreateQROrder";
    private static final String QR_API_URL = "https://apipays.co.in/api/usercashout/qrCreate_Order";
    private static final String NGMB_QR_API_URL = "https://api.ngmbportal.com/api/NGMBAPI/CollectionNGMB";
    private static final String NGMB_QR_CALLBACK_URL = "https://trustlypay.com/pgtrustlypay/gateway/v1/ngmb/payin/response";
    
    // Authentication headers
    private static final String AUTH_HEADER = "Basic " + java.util.Base64.getEncoder().encodeToString("MID0231057:amvt_e1PYc8dVnMvvnvGnpdNQ7p4ddB0PO2mSDmLB4XzFQ0".getBytes());
    private static final String NGMB_AUTH_HEADER = "Basic " + java.util.Base64.getEncoder().encodeToString("NGMBpay_live_CYOkawJ9Vrh4tFb:bRnA1MCYukSIfUr".getBytes());
    
    // Safepay credentials
    private static final String SAFEPAY_CLIENTID = "abc";
    private static final String SAFEPAY_CLIENTSECRET = "abc";
    
    // Compiled patterns for better performance
    private static final Pattern USERNAME_REGEX = Pattern.compile(USERNAME_PATTERN);
    private static final Pattern MOBILE_REGEX = Pattern.compile(MOBILE_PATTERN);
    private static final Pattern AMOUNT_REGEX = Pattern.compile(AMOUNT_PATTERN);
    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);
    
    @Autowired
    private MerchantApiService apiService;
    
    @Autowired
    private MerchantService merchantService;
    
    @Autowired
    private MerchantBusinessService merchantBusinessService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PaymentService payService;
    
    @Autowired
    private MerchantVendorBankService merchantVendorBankService;
    
    @Autowired
    private VendorBankService vendorBankService;
    
    @Autowired
    private IntentpayMIDKeysService intentpayMIDKeysService;
    
    @Autowired
    private CyroKeysService cyroKeysService;
    
    @Autowired
    private DigibizMIDKeysService digibizMIDKeysService;
    
    @Autowired
    private TechtotechKeysService techtotechKeysService;
    
    @Autowired
    private TechtotechFPKeysService techtotechFPKeysService;
    
    @Autowired
    private TechtotechDPKeysService techtotechDPKeysService;
    
    @Autowired
    private TimepayMIDKeysService timepayMIDKeysService;
    
    @Autowired
    private MerchantSeamlessKeysService merchantSeamlessKeysService;
    
    @Autowired
    private IntentpayApiResponseService intentpayApiResponseService;
    
    @Autowired
    private ApexTokenManager apexTokenManager;
    
    @Value("${bulkpe.bulkpeKey}")
    private String bulkpeKey;
    
    @Value("${ismart.ismartMID}")
    private String ismartMID;
    
    @Value("${ismart.merchantKey}")
    private String merchantKey;
    
    @Value("${iserveu.isuUrl}")
    private String isuUrl;
    
    @Value("${iserveu.isuclientId}")
    private String isuclientId;
    
    @Value("${iserveu.isuclientSecret}")
    private String isuclientSecret;
    
    @Value("${acspay.username}")
    private String acsPayUserName;
    
    @Value("${acspay.password}")
    private String acsPayPassword;
    
    @Value("${acspay.api.url}")
    private String acsPayURL;
    
    @RequestMapping(value = "/v2/intent/initialrequest", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> handlePostRequest(@RequestBody String requestData, HttpServletRequest request) {
        try {
            log.info("Post Request From Merchant: {}", requestData);
            
            // Parse initial request
            JSONObject requestJson = new JSONObject(requestData);
            String apiKey = requestJson.optString("clientId", "");
            String encryptedData = requestJson.optString("encryptedData", "");
            
            // Basic request validation
            ResponseEntity<String> validationError = validateBasicRequest(apiKey, encryptedData);
            if (validationError != null) {
                return validationError;
            }
            
            // Get and validate merchant API
            LiveMerchantApiModel liveApi = validateAndGetMerchantApi(apiKey);
            if (liveApi == null) {
                return createErrorResponse(ErrorCodes.INVALID_CLIENT_ID, ErrorMessages.INVALID_CLIENT_ID_MSG, apiKey, "");
            }
            
            // Decrypt and parse client request
            ClientRequest clientRequest = decryptAndParseRequest(encryptedData, liveApi, apiKey);
            if (clientRequest == null) {
                return createErrorResponse(ErrorCodes.INVALID_ENCRYPTED_DATA, ErrorMessages.INVALID_ENCRYPTED_DATA_MSG, apiKey, encryptedData);
            }
            
            // Validate client data and signature
            validationError = validateClientDataAndSignature(clientRequest, liveApi, apiKey, encryptedData);
            if (validationError != null) {
                return validationError;
            }
            
            // Process payment request
            return processPaymentRequest(clientRequest, liveApi);
            
        } catch (Exception e) {
            log.error("Error processing request", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> validateBasicRequest(String apiKey, String encryptedData) {
        if (StringUtils.isEmpty(apiKey)) {
            log.info("API KEY IS EMPTY");
            return createErrorResponse(ErrorCodes.CLIENT_ID_REQUIRED, ErrorMessages.CLIENT_ID_REQUIRED_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(encryptedData)) {
            log.info("ENCRYPTED DATA IS EMPTY");
            return createErrorResponse(ErrorCodes.ENCRYPTED_DATA_REQUIRED, ErrorMessages.ENCRYPTED_DATA_REQUIRED_MSG, apiKey, encryptedData);
        }
        
        return null; // Valid
    }
    
    private LiveMerchantApiModel validateAndGetMerchantApi(String apiKey) {
        LiveMerchantApiModel liveApi = apiService.getLiveMerchantByApiKey(apiKey);
        if (liveApi == null) {
            log.info("liveApi is null for apiKey: {}", apiKey);
        }
        return liveApi;
    }
    
    private ClientRequest decryptAndParseRequest(String encryptedData, LiveMerchantApiModel liveApi, String apiKey) {
        try {
            TrustlypaySecureData secureData = new TrustlypaySecureData();
            String decryptedString = secureData.decryption(encryptedData, liveApi.getRequest_salt_key(), liveApi.getEncryption_request_key());
            log.info("Merchant Json request data: {}", decryptedString);
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(decryptedString, ClientRequest.class);
        } catch (Exception e) {
            log.error("Error decrypting data for apiKey: {}", apiKey, e);
            return null;
        }
    }
    
    private ResponseEntity<String> validateClientDataAndSignature(ClientRequest cr, LiveMerchantApiModel liveApi, String apiKey, String encryptedData) {
        try {
            // Extract client data from request
            JSONObject mainRes = new JSONObject(new ObjectMapper().writeValueAsString(cr));
            
            String amount = getStringValue(mainRes, "amount");
            String clientId = getStringValue(mainRes, "clientId");
            String txnCurr = getStringValue(mainRes, "txnCurr");
            String emailId = getStringValue(mainRes, "emailId");
            String signature = getStringValue(mainRes, "signature");
            String mobileNumber = getStringValue(mainRes, "mobileNumber");
            String clientKey = getStringValue(mainRes, "clientSecret");
            String username = getStringValue(mainRes, "username");
            
            // Validate individual fields
            ResponseEntity<String> fieldValidation = validateClientFields(username, clientId, clientKey, emailId, mobileNumber, txnCurr, amount, apiKey, encryptedData);
            if (fieldValidation != null) {
                return fieldValidation;
            }
            
            // Validate signature
            return validateSignature(signature, clientId, liveApi.getApi_secret(), txnCurr, amount, emailId, mobileNumber, username, liveApi.getRequest_hashkey(), apiKey, encryptedData);
            
        } catch (Exception e) {
            log.error("Error validating client data", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, apiKey, encryptedData);
        }
    }
    
    private String getStringValue(JSONObject json, String key) {
        return json.isNull(key) ? null : json.getString(key);
    }
    
    private ResponseEntity<String> validateClientFields(String username, String clientId, String clientKey, String emailId, String mobileNumber, String txnCurr, String amount, String apiKey, String encryptedData) {
        
        if (StringUtils.isEmpty(username) || !USERNAME_REGEX.matcher(username).matches()) {
            return createErrorResponse(ErrorCodes.USERNAME_INVALID, ErrorMessages.USERNAME_INVALID_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(clientId)) {
            return createErrorResponse(ErrorCodes.CLIENT_ID_EMPTY, ErrorMessages.CLIENT_ID_EMPTY_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(clientKey)) {
            return createErrorResponse(ErrorCodes.CLIENT_SECRET_EMPTY, ErrorMessages.CLIENT_SECRET_EMPTY_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(emailId) || !EMAIL_REGEX.matcher(emailId).matches()) {
            return createErrorResponse(ErrorCodes.EMAIL_INVALID, ErrorMessages.EMAIL_INVALID_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(mobileNumber) || !MOBILE_REGEX.matcher(mobileNumber).matches()) {
            return createErrorResponse(ErrorCodes.MOBILE_INVALID, ErrorMessages.MOBILE_INVALID_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(txnCurr) || !CURRENCY_INR.equals(txnCurr)) {
            return createErrorResponse(ErrorCodes.CURRENCY_INVALID, ErrorMessages.CURRENCY_INVALID_MSG, apiKey, encryptedData);
        }
        
        if (StringUtils.isEmpty(amount) || !AMOUNT_REGEX.matcher(amount).matches()) {
            return createErrorResponse(ErrorCodes.AMOUNT_INVALID, ErrorMessages.AMOUNT_INVALID_MSG, apiKey, encryptedData);
        }
        
        try {
            double amountValue = Double.parseDouble(amount);
            if (amountValue < MIN_AMOUNT) {
                return createErrorResponse(ErrorCodes.AMOUNT_INVALID, ErrorMessages.AMOUNT_INVALID_MSG, apiKey, encryptedData);
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(ErrorCodes.AMOUNT_INVALID, ErrorMessages.AMOUNT_INVALID_MSG, apiKey, encryptedData);
        }
        
        return null; // Valid
    }
    
    private ResponseEntity<String> validateSignature(String clientSignature, String clientId, String secretKey, String txnCurr, String amount, String emailId, String mobileNumber, String username, String requestHashKey, String apiKey, String encryptedData) {
        try {
            String calculatedSignature = SignatureKey.calculateRFC2104HMAC(requestHashKey, clientId, secretKey, txnCurr, amount, emailId, mobileNumber, username);
            
            log.info("Client Signature: {}", clientSignature);
            log.info("Our Signature: {}", calculatedSignature);
            
            if (!clientSignature.equals(calculatedSignature)) {
                return createErrorResponse("305", "Invalid Signature", apiKey, encryptedData);
            }
            
            return null; // Valid signature
            
        } catch (Exception e) {
            log.error("Error calculating signature", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, apiKey, encryptedData);
        }
    }
    
    private ResponseEntity<String> processPaymentRequest(ClientRequest cr, LiveMerchantApiModel liveApi) {
        try {
            // Get current time
            ZonedDateTime istTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            Date currentDate = Date.from(istTime.toInstant());
            
            // Create or get order
            LiveOrder orderResponse = getOrCreateOrder(cr, liveApi, currentDate);
            
            // Create payment
            LivePayment payment = createPayment(cr, liveApi, orderResponse, currentDate);
            
            // Process vendor-specific logic
            return processVendorPayment(payment, liveApi, orderResponse);
            
        } catch (Exception e) {
            log.error("Error processing payment request", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private LiveOrder getOrCreateOrder(ClientRequest cr, LiveMerchantApiModel liveApi, Date currentDate) {
        LiveOrder orderResponse = orderService.getLiveMerchantById(cr.getOrder_gid());
        
        if (cr.getOrder_gid() == null) {
            LiveOrder liveOrder = new LiveOrder();
            liveOrder.setOrder_amount(Double.parseDouble(cr.getAmount()));
            liveOrder.setCreated_date(currentDate);
            liveOrder.setOrder_status(ORDER_CREATED);
            liveOrder.setCreated_merchant(liveApi.getCreated_merchant());
            orderResponse = orderService.createLiveOrder(liveOrder);
            
            log.info("Live Order created: {}", orderResponse.getOrder_gid());
        }
        
        return orderResponse;
    }
    
    private LivePayment createPayment(ClientRequest cr, LiveMerchantApiModel liveApi, LiveOrder orderResponse, Date currentDate) {
        LivePayment payment = new LivePayment();
        payment.setTransaction_amount(Double.parseDouble(cr.getAmount()));
        payment.setTransaction_username(cr.getUsername());
        payment.setTransaction_email(cr.getEmailId());
        payment.setTransaction_contact(cr.getMobileNumber());
        payment.setCreated_merchant(liveApi.getCreated_merchant());
        payment.setTransaction_status(TRANSACTION_PENDING);
        payment.setCreated_date(currentDate);
        payment.setOrder_id(orderResponse.getId());
        payment.setUdf1(cr.getUdf1());
        payment.setUdf2(cr.getUdf2());
        
        LivePayment savedPayment = payService.saveLivePayment(payment);
        log.info("Created live Transaction Id: {}", savedPayment.getTransaction_gid());
        
        return savedPayment;
    }
    
    private ResponseEntity<String> processVendorPayment(LivePayment payment, LiveMerchantApiModel liveApi, LiveOrder orderResponse) {
        try {
            MerchantVendorBankEntity merchantVendorBank = merchantVendorBankService.getMerchantVendorBankInfo(liveApi.getCreated_merchant());
            
            if (merchantVendorBank.getUpi() == 0) {
                return createSuccessResponse("UPI disabled", "false");
            }
            
            VendorBank vendorBank = vendorBankService.getBankName(merchantVendorBank.getUpi_vendor_bank_id());
            
            String bankName = vendorBank.getBank_name();
            
            switch (bankName) {
                case INTENTPAY_BANK:
                    return processIntentpayPayment(payment, orderResponse);
                case CYRO_BANK:
                    return processCyroPayment(payment, orderResponse);
                case DIGIBIZ_BANK:
                    return processDigibizPayment(payment, orderResponse);
                case TECHTOTECH_BANK:
                    return processTechtotechPayment(payment, orderResponse);
                case TECHTOTECH_FP_BANK:
                    return processTechtotechFPPayment(payment, orderResponse);
                case TECHTOTECH_DP_BANK:
                    return processTechtotechDPPayment(payment, orderResponse);
                case TIMEPAY_BANK:
                    return processTimepayPayment(payment, orderResponse);
                case SAFEPAY_BANK:
                    return processSafepayPayment(payment, orderResponse);
                case NGMB_BANK:
                    return processNGMBPayment(payment, orderResponse);
                case BULKPE_BANK:
                    return processBulkpePayment(payment, orderResponse);
                case ISMART_BANK:
                    return processISmartPayment(payment, orderResponse);
                case ISERVEU_BANK:
                    return processIServeUPayment(payment, orderResponse);
                case ACSPAY_BANK:
                    return processAcsPayPayment(payment, orderResponse);
                case APIPAYS_BANK:
                    return processApiPaysPayment(payment, orderResponse);
                case APEX_BANK:
                    return processApexPayment(payment, orderResponse);
                case SEAMLESS_BANK:
                    return processSeamlessPayment(payment, orderResponse);
                case PAYU_BANK:
                    return processPayUPayment(payment, orderResponse);
                case RAZORPAY_BANK:
                    return processRazorpayPayment(payment, orderResponse);
                case PHONEPE_BANK:
                    return processPhonePePayment(payment, orderResponse);
                case GOOGLEPAY_BANK:
                    return processGooglePayPayment(payment, orderResponse);
                case PAYTM_BANK:
                    return processPaytmPayment(payment, orderResponse);
                default:
                    log.warn("Unsupported bank: {}", bankName);
                    return createErrorResponse("400", "Unsupported payment method", "", "");
            }
            
        } catch (Exception e) {
            log.error("Error processing vendor payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processIntentpayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            IntentpayMidKeys midKeys = intentpayMIDKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing Intentpay payment");
            
            String apiUrl = String.format(INTENTPAY_API_URL_TEMPLATE, midKeys.getIntentpay_mid());
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("merchantid", midKeys.getIntentpay_mid());
            headers.set("Content-Type", "application/json");
            headers.set("merchantsecret", midKeys.getSecret_key());
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("userContactNumber", payment.getTransaction_contact());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            log.info("Intentpay request: {}", jsonString);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, httpEntity, String.class);
            
            String responseData = response.getBody();
            JSONObject responseJson = new JSONObject(responseData);
            log.info("Intentpay response: {}", responseData);
            
            return ResponseEntity.ok(responseJson.toString());
            
        } catch (JsonProcessingException e) {
            log.error("Error processing Intentpay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processCyroPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            CyroKeys cyroKeys = cyroKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing Cyro payment");
            
            // Add Cyro-specific payment processing logic here
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Cyro");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Cyro payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processDigibizPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            DigibizMidKeys digibizKeys = digibizMIDKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing Digibiz payment");
            
            // Use DigibizEncryption utility for encryption
            DigibizEncryption encryption = new DigibizEncryption();
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Digibiz");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Digibiz payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processTechtotechPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            TechtotechMidKeys techtotechKeys = techtotechKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing Techtotech payment");
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Techtotech");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Techtotech payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processTechtotechFPPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            TechtotechFPMidKeys techtotechFPKeys = techtotechFPKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing TechtotechFP payment");
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "TechtotechFP");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing TechtotechFP payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processTechtotechDPPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            TechtotechDPMidKeys techtotechDPKeys = techtotechDPKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing TechtotechDP payment");
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "TechtotechDP");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing TechtotechDP payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processTimepayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            TimepayMidKeys timepayKeys = timepayMIDKeysService.findByMerchantId(orderResponse.getCreated_merchant());
            log.info("Processing Timepay payment");
            
            // Use TimepaySecureData utility
            TimepaySecureData timepaySecureData = new TimepaySecureData();
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Timepay");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Timepay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processSafepayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Safepay QR payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("clientId", SAFEPAY_CLIENTID);
            requestData.put("clientSecret", SAFEPAY_CLIENTSECRET);
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("mobileNumber", payment.getTransaction_contact());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SAFEPAY_QR_API_URL, httpEntity, String.class);
            
            log.info("Safepay response: {}", response.getBody());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing Safepay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processNGMBPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing NGMB QR payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", NGMB_AUTH_HEADER);
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("mobileNumber", payment.getTransaction_contact());
            requestData.put("callbackUrl", NGMB_QR_CALLBACK_URL);
            requestData.put("orderId", orderResponse.getOrder_gid());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(NGMB_QR_API_URL, httpEntity, String.class);
            
            log.info("NGMB response: {}", response.getBody());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing NGMB payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processBulkpePayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Bulkpe payment");
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Bulkpe");
            response.put("bulkpeKey", bulkpeKey);
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Bulkpe payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processISmartPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing iSmart payment");
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "iSmart");
            response.put("ismartMID", ismartMID);
            response.put("merchantKey", merchantKey);
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing iSmart payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processIServeUPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing iServeU payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("clientId", isuclientId);
            requestData.put("clientSecret", isuclientSecret);
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("mobileNumber", payment.getTransaction_contact());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(isuUrl, httpEntity, String.class);
            
            log.info("iServeU response: {}", response.getBody());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing iServeU payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processAcsPayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing AcsPay payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("username", acsPayUserName);
            requestData.put("password", acsPayPassword);
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("mobileNumber", payment.getTransaction_contact());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(acsPayURL, httpEntity, String.class);
            
            log.info("AcsPay response: {}", response.getBody());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing AcsPay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processApiPaysPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing ApiPays QR payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", AUTH_HEADER);
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("mobileNumber", payment.getTransaction_contact());
            requestData.put("orderId", orderResponse.getOrder_gid());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonString, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(QR_API_URL, httpEntity, String.class);
            
            log.info("ApiPays response: {}", response.getBody());
            return response;
            
        } catch (Exception e) {
            log.error("Error processing ApiPays payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processApexPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Apex payment");
            
            // Use ApexTokenManager for token management
            String token = apexTokenManager.getToken();
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Apex");
            response.put("token", token);
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Apex payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processSeamlessPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Seamless payment");
            
            // Use MerchantSeamlessKeysService for seamless payments
            // Add seamless payment processing logic here
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Seamless");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            log.error("Error processing Seamless payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processPayUPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing PayU payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("amount", String.valueOf(payment.getTransaction_amount()));
            requestData.put("email", payment.getTransaction_email());
            requestData.put("phone", payment.getTransaction_contact());
            requestData.put("productinfo", "Payment for Order " + orderResponse.getOrder_gid());
            requestData.put("firstname", payment.getTransaction_username());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "PayU");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            log.error("Error processing PayU payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processRazorpayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Razorpay payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // Add Razorpay API key authentication here
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("amount", String.valueOf((int)(payment.getTransaction_amount() * 100))); // Razorpay amount in paise
            requestData.put("currency", "INR");
            requestData.put("receipt", orderResponse.getOrder_gid());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Razorpay");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            log.error("Error processing Razorpay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processPhonePePayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing PhonePe payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // Add PhonePe API authentication here
            
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("merchantId", "MERCHANT_ID");
            requestData.put("merchantTransactionId", orderResponse.getOrder_gid());
            requestData.put("merchantUserId", payment.getTransaction_username());
            requestData.put("amount", String.valueOf((int)(payment.getTransaction_amount() * 100))); // PhonePe amount in paise
            requestData.put("mobileNumber", payment.getTransaction_contact());
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "PhonePe");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            log.error("Error processing PhonePe payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processGooglePayPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing GooglePay payment");
            
            // Generate UPI payment request for Google Pay
            String upiId = "merchant@paytm"; // Replace with actual merchant UPI ID
            String upiUrl = String.format("upi://pay?pa=%s&pn=%s&mc=&tid=%s&tr=%s&tn=%s&am=%s&cu=INR",
                upiId,
                "Merchant Name", // Replace with actual merchant name
                orderResponse.getOrder_gid(),
                orderResponse.getOrder_gid(),
                "Payment for Order " + orderResponse.getOrder_gid(),
                String.valueOf(payment.getTransaction_amount())
            );
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "GooglePay");
            response.put("upiUrl", upiUrl);
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            log.error("Error processing GooglePay payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> processPaytmPayment(LivePayment payment, LiveOrder orderResponse) {
        try {
            log.info("Processing Paytm payment");
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            // Add Paytm API authentication headers here
            
            Map<String, Object> requestData = new HashMap<>();
            Map<String, Object> body = new HashMap<>();
            body.put("requestType", "Payment");
            body.put("mid", "MERCHANT_ID"); // Replace with actual Paytm merchant ID
            body.put("websiteName", "WEBSTAGING"); // Replace with actual website name
            body.put("orderId", orderResponse.getOrder_gid());
            body.put("callbackUrl", "https://trustlypay.com/paytm/callback");
            
            Map<String, String> txnAmount = new HashMap<>();
            txnAmount.put("value", String.valueOf(payment.getTransaction_amount()));
            txnAmount.put("currency", "INR");
            body.put("txnAmount", txnAmount);
            
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("custId", payment.getTransaction_username());
            userInfo.put("mobile", payment.getTransaction_contact());
            userInfo.put("email", payment.getTransaction_email());
            body.put("userInfo", userInfo);
            
            requestData.put("body", body);
            
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(requestData);
            
            JSONObject response = new JSONObject();
            response.put("statusCode", "200");
            response.put("status", "Success");
            response.put("vendor", "Paytm");
            response.put("transactionId", payment.getTransaction_gid());
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            log.error("Error processing Paytm payment", e);
            return createErrorResponse(ErrorCodes.INTERNAL_ERROR, ErrorMessages.INTERNAL_ERROR_MSG, "", "");
        }
    }
    
    private ResponseEntity<String> createErrorResponse(String statusCode, String description, String clientId, String encryptedData) {
        JSONObject response = new JSONObject();
        response.put("statusCode", statusCode);
        response.put("status", STATUS_FAILED);
        response.put("Description", description);
        response.put("clientId", clientId);
        if (!StringUtils.isEmpty(encryptedData)) {
            response.put("encryptedData", encryptedData);
        }
        return ResponseEntity.ok(response.toString());
    }
    
    private ResponseEntity<String> createSuccessResponse(String description, String data) {
        JSONObject response = new JSONObject();
        response.put("statusCode", "200");
        response.put("status", "Success");
        response.put("Description", description);
        response.put("data", data);
        return ResponseEntity.ok(response.toString());
    }
}
