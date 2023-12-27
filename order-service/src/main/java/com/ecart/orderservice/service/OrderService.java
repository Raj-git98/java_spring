package com.ecart.orderservice.service;

import com.ecart.orderservice.dto.InventoryResponse;
import com.ecart.orderservice.dto.OrderLineItemsDto;
import com.ecart.orderservice.dto.OrderRequest;
import com.ecart.orderservice.model.Order;
import com.ecart.orderservice.model.OrderLineItems;
import com.ecart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient webClient;
    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

       List<OrderLineItems> orderLineItems =  orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

       order.setOrderLineItemsList(orderLineItems);

       List<String> skucodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();
       //Call to Inventory Service

       InventoryResponse[] inventoryResponseArray = webClient.get()
               .uri("http://localhost:8082/api/inventory",
                       uriBuilder -> uriBuilder.queryParam("skuCode", skucodes).build())
               .retrieve()
               .bodyToMono(InventoryResponse[].class)
               .block();

        boolean allProducts = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

       if(allProducts){
           orderRepository.save(order);
       }else {
            throw  new IllegalArgumentException("Product is out of stock, please try again later");
       }


    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }
}
