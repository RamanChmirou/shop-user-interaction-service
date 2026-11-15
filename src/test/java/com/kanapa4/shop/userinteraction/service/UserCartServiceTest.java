package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.CartServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartItemResponse;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.PriceChangeDto;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.SelectedOptionRequest;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.dto.VatRate;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCartServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID ITEM_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private UserCartService userCartService;

    @BeforeEach
    void setUp() {
        when(currentUser.id()).thenReturn(USER_ID);
    }

    private CartResponse sampleCart() {
        CartItemResponse item = new CartItemResponse(
                ITEM_ID,
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                1,
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        return new CartResponse(
                CART_ID,
                USER_ID,
                "ACTIVE",
                List.of(item),
                1,
                new BigDecimal("3999.00"),
                "PLN"
        );
    }

    @Test
    @DisplayName("getCart retrieves cart for authenticated user")
    void getCart_Success() {
        CartResponse expectedCart = sampleCart();
        when(cartServiceClient.getCart(USER_ID)).thenReturn(expectedCart);

        CartResponse result = userCartService.getCart();

        assertThat(result).isEqualTo(expectedCart);
        verify(cartServiceClient).getCart(USER_ID);
    }

    @Test
    @DisplayName("addItem adds product item to user's cart")
    void addItem_Success() {
        AddCartItemRequest request = new AddCartItemRequest(PRODUCT_ID, 2, List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        CartResponse expectedCart = sampleCart();
        when(cartServiceClient.addItem(USER_ID, request)).thenReturn(expectedCart);

        CartResponse result = userCartService.addItem(request);

        assertThat(result).isEqualTo(expectedCart);
        verify(cartServiceClient).addItem(USER_ID, request);
    }

    @Test
    @DisplayName("updateQuantity updates item quantity for user's cart")
    void updateQuantity_Success() {
        UpdateQuantityRequest request = new UpdateQuantityRequest(3);
        CartResponse expectedCart = sampleCart();
        when(cartServiceClient.updateQuantity(USER_ID, ITEM_ID, request)).thenReturn(expectedCart);

        CartResponse result = userCartService.updateQuantity(ITEM_ID, request);

        assertThat(result).isEqualTo(expectedCart);
        verify(cartServiceClient).updateQuantity(USER_ID, ITEM_ID, request);
    }

    @Test
    @DisplayName("removeItem removes item from user's cart")
    void removeItem_Success() {
        userCartService.removeItem(ITEM_ID);

        verify(cartServiceClient).removeItem(USER_ID, ITEM_ID);
    }

    @Test
    @DisplayName("clear empties user's cart")
    void clear_Success() {
        userCartService.clear();

        verify(cartServiceClient).clear(USER_ID);
    }

    @Test
    @DisplayName("reprice recalculates cart against latest prices")
    void reprice_Success() {
        RepriceResponse expectedResponse = new RepriceResponse(
                sampleCart(),
                List.of()
        );
        when(cartServiceClient.reprice(USER_ID)).thenReturn(expectedResponse);

        RepriceResponse result = userCartService.reprice();

        assertThat(result).isEqualTo(expectedResponse);
        verify(cartServiceClient).reprice(USER_ID);
    }
}
