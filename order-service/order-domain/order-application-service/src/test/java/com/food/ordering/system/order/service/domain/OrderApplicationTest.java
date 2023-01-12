package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.*;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.dto.create.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.input.service.OrderApplicationService;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = OrderTestConfiguration.class)
class OrderApplicationTest {

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OrderDataMapper orderDataMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private CreateOrderCommand createOrderCommand;

    private CreateOrderCommand createOrderCommandWrongPrice;

    private CreateOrderCommand createOrderCommandWrongProductPrice;

    private static final UUID CUSTOMER_ID = UUID.fromString("f49400ba-529c-4e1f-8493-b0e880c0b3bb");

    private static final UUID RESTAURANT_ID = UUID.fromString("e2259847-274b-4e9c-afe1-0b0c5dd98636");

    private static final UUID PRODUCT_ID = UUID.fromString("7406dd7b-a796-41b0-af89-17bc03399f60");

    private static final UUID ORDER_ID = UUID.fromString("2c0b100a-3c2c-4b67-a3a2-c6b53d067d17");

    private final BigDecimal PRICE = new BigDecimal("200.00");

    @BeforeAll
    public void init() {
        createOrderCommand = CreateOrderCommand.builder()
            .customerID(CUSTOMER_ID)
            .restaurantId(RESTAURANT_ID)
            .address(OrderAddress.builder()
                .street("street_1")
                .postalCode("1000AB")
                .city("Paris")
                .build())
            .price(PRICE)
            .items(List.of(
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(1)
                    .price(new BigDecimal("50.00"))
                    .subTotal(new BigDecimal("50.00"))
                    .build(),
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(3)
                    .price(new BigDecimal("50.00"))
                    .subTotal(new BigDecimal("150.00"))
                    .build()
            ))
            .build();

        createOrderCommandWrongPrice = CreateOrderCommand.builder()
            .customerID(CUSTOMER_ID)
            .restaurantId(RESTAURANT_ID)
            .address(OrderAddress.builder()
                .street("street_1")
                .postalCode("1000AB")
                .city("Paris")
                .build())
            .price(new BigDecimal("250.00"))
            .items(List.of(
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(1)
                    .price(new BigDecimal("50.00"))
                    .subTotal(new BigDecimal("50.00"))
                    .build(),
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(3)
                    .price(new BigDecimal("50.00"))
                    .subTotal(new BigDecimal("150.00"))
                    .build()
            ))
            .build();

        createOrderCommandWrongProductPrice = CreateOrderCommand.builder()
            .customerID(CUSTOMER_ID)
            .restaurantId(RESTAURANT_ID)
            .address(OrderAddress.builder()
                .street("street_1")
                .postalCode("1000AB")
                .city("Paris")
                .build())
            .price(new BigDecimal("210.00"))
            .items(List.of(
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(1)
                    .price(new BigDecimal("60.00"))
                    .subTotal(new BigDecimal("60.00"))
                    .build(),
                OrderItem.builder()
                    .productID(PRODUCT_ID)
                    .quantity(3)
                    .price(new BigDecimal("50.00"))
                    .subTotal(new BigDecimal("150.00"))
                    .build()
            ))
            .build();

        Customer customer = new Customer();
        customer.setId(new CustomerId(CUSTOMER_ID));

        Restaurant restaurantResponse = Restaurant.Builder.builder()
            .restaurantId(new RestaurantId(RESTAURANT_ID))
            .products(List.of(
                new Product(new ProductId(PRODUCT_ID), "product-1", new Money(new BigDecimal("50.00"))),
                new Product(new ProductId(PRODUCT_ID), "product-2", new Money(new BigDecimal("50.00")))))
            .active(true)
            .build();

        Order order = orderDataMapper.createOrderCommandToOrder(createOrderCommand);
        order.setId(new OrderId(ORDER_ID));

        Mockito.when(customerRepository.findCustomer(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        Mockito.when(restaurantRepository.findRestaurantInformation(
                orderDataMapper.createOrderCommandToRestaurant(createOrderCommand)))
            .thenReturn(Optional.of(restaurantResponse));
        Mockito.when(orderRepository.save(Mockito.any(Order.class))).thenReturn(order);
    }

    @Test
    void testCreateOrder() {
        CreateOrderResponse createOrderResponse = orderApplicationService.createOrder(createOrderCommand);
        assertEquals(OrderStatus.PENDING, createOrderResponse.getOrderStatus());
        assertEquals("Order Created Successfully", createOrderResponse.getMessage());
        assertNotNull(createOrderResponse.getOrderTrackingID());
    }

    @Test
    void testCreateOrderWithWrongTotalPrice() {
        OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
            () -> orderApplicationService.createOrder(createOrderCommandWrongPrice));
        assertEquals("Total price: 250.00 is not equal to Order items total: 200.00!",
            orderDomainException.getMessage());
    }

    @Test
    void testCreateOrderWithWrongProductPrice() {
        OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
            () -> orderApplicationService.createOrder(createOrderCommandWrongProductPrice));
        assertEquals("Order item price: 60.00 is not valid for product " + PRODUCT_ID,
            orderDomainException.getMessage());
    }

    @Test
    void testCreateOrderWithPassiveRestaurant() {
        Restaurant restaurantResponse = Restaurant.Builder.builder()
            .restaurantId(new RestaurantId(RESTAURANT_ID))
            .products(List.of(
                new Product(new ProductId(PRODUCT_ID), "product-1", new Money(new BigDecimal("50.00"))),
                new Product(new ProductId(PRODUCT_ID), "product-2", new Money(new BigDecimal("50.00")))))
            .active(false)
            .build();

        Mockito.when(restaurantRepository.findRestaurantInformation(
                orderDataMapper.createOrderCommandToRestaurant(createOrderCommand)))
            .thenReturn(Optional.of(restaurantResponse));

        OrderDomainException orderDomainException = assertThrows(OrderDomainException.class,
            () -> orderApplicationService.createOrder(createOrderCommand));
        assertEquals("Restaurant with id " + RESTAURANT_ID + " is currently not active!",
            orderDomainException.getMessage());
    }
}
