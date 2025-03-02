package io.github.alexshamrai.e2e;

import io.github.alexshamrai.client.BookClient;
import io.github.alexshamrai.client.OrderClient;
import io.github.alexshamrai.dto.*;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.alexshamrai.e2e.BaseTest.BOOK_SERVICE_URL;
import static io.github.alexshamrai.e2e.BaseTest.ORDER_SERVICE_URL;
import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {
    private final BookClient bookClient = new BookClient(BOOK_SERVICE_URL);
    private final OrderClient orderClient = new OrderClient(ORDER_SERVICE_URL);
    private BookDto testBook;
    private int baseStockQuantity = 10;

    @BeforeEach
    void setUp() {
        orderClient.deleteAllOrders()
            .then()
            .statusCode(204);

        bookClient.deleteAllBooks()
            .then()
            .statusCode(204);

        BookDto bookRequest = BookDto.builder()
            .title("Test Book")
            .author("Test Author")
            .price(29.99)
            .stockQuantity(baseStockQuantity)
            .build();

        Response createBookResponse = bookClient.createBook(bookRequest);
        createBookResponse.then().statusCode(200);
        testBook = createBookResponse.as(BookDto.class);
    }

    @Test
    void createUpdateDeleteOrder(){
        // Arrange
        int countForPurchasing = 1;
        long userId = 11111;
        var baseStatus = "PENDING";
        var newStatus = "ACCEPTED";
        OrderItemRequest orderItemRequest = OrderItemRequest.builder()
                .bookId(testBook.getId())
                .quantity(countForPurchasing)
                .build();

        OrderRequest orderRequest = OrderRequest.builder()
                .userId(userId)
                .items(List.of(orderItemRequest))
                .build();

        // Act & Assert
        // 1. Create an Order for the Book
        Response createOrderResponse = orderClient.createOrder(orderRequest);
        createOrderResponse.then().statusCode(200);
        OrderDto order = createOrderResponse.as(OrderDto.class);
        assertThat(order.getStatus()).isEqualTo(baseStatus);
        assertThat(order.getTotalAmount()).isEqualTo(testBook.getPrice() * countForPurchasing);

        // 2. Validate Book Stock Count
        Response getBookResponse = bookClient.getBook(testBook.getId());
        getBookResponse.then().statusCode(200);
        testBook = getBookResponse.as(BookDto.class);
        assertThat(testBook.getStockQuantity()).isEqualTo(baseStockQuantity - countForPurchasing);

        // 3. Update Order Status to "ACCEPTED"
        Response updateOrderResponse = orderClient.updateOrderStatus(order.getId(), newStatus);
        updateOrderResponse.then().statusCode(200);
        order = updateOrderResponse.as(OrderDto.class);
        assertThat(order.getStatus()).isEqualTo(newStatus);

        // 4. Delete Order
        Response deleteResponse = orderClient.deleteOrder(order.getId());
        deleteResponse.then().statusCode(204);

        // 5. Order is Deleted
        Response getUserOrdersResponse = orderClient.getUserOrders(userId);
        getUserOrdersResponse.then().statusCode(200);
        List<OrderDto> booksBeforeDelete = getUserOrdersResponse.as(new TypeRef<List<OrderDto>>() {});
        assertThat(booksBeforeDelete).isEmpty();
    }
}