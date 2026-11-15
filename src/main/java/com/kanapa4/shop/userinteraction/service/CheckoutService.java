package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.CartServiceClient;
import com.kanapa4.shop.userinteraction.client.OrderServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.CheckoutSummaryResponse;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderCheckoutRequest;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.exception.CheckoutDraftIncompleteException;
import com.kanapa4.shop.userinteraction.exception.CheckoutDraftMissingException;
import com.kanapa4.shop.userinteraction.exception.EmptyCartException;
import com.kanapa4.shop.userinteraction.model.AddressData;
import com.kanapa4.shop.userinteraction.model.CheckoutData;
import com.kanapa4.shop.userinteraction.model.CheckoutDraft;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.repository.CheckoutDraftRepository;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final String DEFAULT_CURRENCY = "PLN";
    private static final List<String> ALL_DRAFT_FIELDS = List.of(
            "firstName", "lastName", "email", "deliveryMethod", "paymentMethod", "shippingAddress");

    private final CheckoutDraftRepository checkoutDraftRepository;
    private final CartServiceClient cartServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Optional<CheckoutData> getDraft() {
        UUID userId = currentUser.id();
        log.info("Retrieving checkout draft for user={}", userId);
        return checkoutDraftRepository.findById(userId).map(CheckoutDraft::getData);
    }

    @Transactional
    public CheckoutData saveDraft(CheckoutData data) {
        UUID userId = currentUser.id();
        log.info("Saving checkout draft for user={}", userId);
        CheckoutDraft draft = checkoutDraftRepository.findById(userId)
                .orElseGet(() -> createNewDraft(userId, data));
        draft.setData(data);
        checkoutDraftRepository.save(draft);
        return draft.getData();
    }

    @Transactional(readOnly = true)
    public CheckoutSummaryResponse getSummary() {
        UUID userId = currentUser.id();
        log.info("Generating checkout summary for user={}", userId);
        CartResponse cart = cartServiceClient.getCart(userId);
        CheckoutData data = checkoutDraftRepository.findById(userId).map(CheckoutDraft::getData).orElse(null);
        return buildSummary(cart, data);
    }

    /**
     * Not transactional on purpose: the call to order-service must not hold a database connection.
     * The draft is deleted only after the order exists, so a rejected checkout can be repeated.
     */
    public OrderResponse submitOrder() {
        UUID userId = currentUser.id();
        log.info("Submitting order for user={}", userId);
        CheckoutData data = loadCompleteDraft(userId);
        CartResponse cart = loadNonEmptyCart(userId);
        OrderResponse order = orderServiceClient.checkout(userId, toOrderRequest(data, cart));
        checkoutDraftRepository.deleteById(userId);
        log.info("Order placed successfully with orderNumber={}, draft removed", order.orderNumber());
        return order;
    }

    private CheckoutDraft createNewDraft(UUID userId, CheckoutData data) {
        return new CheckoutDraft(userId, data);
    }

    private CheckoutSummaryResponse buildSummary(CartResponse cart, CheckoutData data) {
        BigDecimal deliveryCost = deliveryCostOf(data);
        BigDecimal grandTotal = cartTotalOf(cart).add(deliveryCost);
        List<String> missingFields = missingFields(data);
        boolean readyToSubmit = !isCartEmpty(cart) && missingFields.isEmpty();
        return new CheckoutSummaryResponse(
                cart, data, deliveryCost, grandTotal, currencyOf(cart), readyToSubmit, missingFields);
    }

    private CheckoutData loadCompleteDraft(UUID userId) {
        CheckoutData data = checkoutDraftRepository.findById(userId)
                .map(CheckoutDraft::getData)
                .orElseThrow(CheckoutDraftMissingException::new);
        List<String> missing = missingFields(data);
        if (!missing.isEmpty()) {
            throw new CheckoutDraftIncompleteException(missing);
        }
        return data;
    }

    private CartResponse loadNonEmptyCart(UUID userId) {
        CartResponse cart = cartServiceClient.getCart(userId);
        if (isCartEmpty(cart)) {
            throw new EmptyCartException();
        }
        return cart;
    }

    private OrderCheckoutRequest toOrderRequest(CheckoutData data, CartResponse cart) {
        AddressDto shipping = toAddressDto(data.shippingAddress());
        AddressDto billing = Optional.ofNullable(data.billingAddress()).map(this::toAddressDto).orElse(shipping);
        return new OrderCheckoutRequest(
                toCustomerDto(data), data.deliveryMethod(), data.paymentMethod(), shipping, billing, cart.totalAmount());
    }

    private CustomerDto toCustomerDto(CheckoutData data) {
        return new CustomerDto(data.firstName(), data.lastName(), data.email(), data.phone());
    }

    private AddressDto toAddressDto(AddressData data) {
        return new AddressDto(data.street(), data.city(), data.postalCode(), data.country());
    }

    private BigDecimal deliveryCostOf(CheckoutData data) {
        return Optional.ofNullable(data)
                .map(CheckoutData::deliveryMethod)
                .map(DeliveryMethod::getCost)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal cartTotalOf(CartResponse cart) {
        return Optional.ofNullable(cart).map(CartResponse::totalAmount).orElse(BigDecimal.ZERO);
    }

    private String currencyOf(CartResponse cart) {
        return Optional.ofNullable(cart).map(CartResponse::currency).orElse(DEFAULT_CURRENCY);
    }

    private boolean isCartEmpty(CartResponse cart) {
        return cart == null || cart.items() == null || cart.items().isEmpty();
    }

    private List<String> missingFields(CheckoutData data) {
        if (data == null) {
            return ALL_DRAFT_FIELDS;
        }
        return Stream.of(
                checkField("firstName", data.firstName()),
                checkField("lastName", data.lastName()),
                checkField("email", data.email()),
                checkField("deliveryMethod", data.deliveryMethod()),
                checkField("paymentMethod", data.paymentMethod()),
                isAddressComplete(data.shippingAddress()) ? Optional.<String>empty() : Optional.of("shippingAddress")
        )
        .flatMap(Optional::stream)
        .toList();
    }

    private Optional<String> checkField(String fieldName, Object value) {
        if (value == null || (value instanceof String str && str.isBlank())) {
            return Optional.of(fieldName);
        }
        return Optional.empty();
    }

    private boolean isAddressComplete(AddressData address) {
        return address != null
                && !isBlank(address.street())
                && !isBlank(address.city())
                && !isBlank(address.postalCode())
                && !isBlank(address.country());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
