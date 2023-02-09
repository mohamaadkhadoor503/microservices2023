package com.project2023.inventoryservice.controller;


import com.project2023.inventoryservice.dto.InventoryResponse;
import com.project2023.inventoryservice.model.Inventory;
import com.project2023.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    //http://localhost:8082/api/inventory/iphone-13,iphone13-red
    //http://localhost:8082/api/inventory?skuCode=iphone-13&skuCode=iphone13-red

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode){
        return inventoryService.isInStock(skuCode);


    }
}
