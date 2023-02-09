package com.project2023.orderservice.dto;


import com.project2023.orderservice.model.OrderLineItems;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class OrderRequest {
    private List<OrderLineItemsDto> orderLineItemsDtoList;


}
