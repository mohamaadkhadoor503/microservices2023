package com.project2023.orderservice.service;

import com.project2023.orderservice.dto.InventoryResponse;
import com.project2023.orderservice.dto.OrderLineItemsDto;
import com.project2023.orderservice.dto.OrderRequest;
import com.project2023.orderservice.model.Order;
import com.project2023.orderservice.model.OrderLineItems;
import com.project2023.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final StreamBridge streamBridge;

    private  final Tracer tracer;
    public String  placeOrder(OrderRequest orderRequest){
        Order order =new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        Span inventoryServiceLookup = tracer.nextSpan().name("inventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope=tracer.withSpan(inventoryServiceLookup.start())){
            //call Inventory Service, and Place order if product is in stock

            InventoryResponse[] inventoryResponsArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",uriBuilder->uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponsArray).allMatch(InventoryResponse::isInStock);

            if(allProductsInStock){
                orderRepository.save(order);
                log.info("sending order details to notification service");
//                streamBridge.send("notificationEventSupplier-out-0 ",order.getId());
                streamBridge.send("notificationEventSupplier-out-0", MessageBuilder.withPayload(order.getId()).build());
                return "Order placed Successfully";
            }
            else{
                throw new IllegalArgumentException("Product is not in stock,please try again later");
            }
        }
        finally {
            inventoryServiceLookup.end();
        }


    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems= new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;



    }
}
