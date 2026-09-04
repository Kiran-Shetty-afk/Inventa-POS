package com.zosh.mapper;

import com.zosh.modal.OrderItem;
import com.zosh.payload.dto.OrderItemDTO;
import com.zosh.payload.dto.ProductDTO;

public class OrderItemMapper {

    public static OrderItemDTO toDto(OrderItem item) {
        if (item == null) return null;

        ProductDTO productDTO = ProductDTO.builder()
                .id(item.getProductId())
                .name(item.getProductName())
                .sku(item.getProductSku())
                .image(item.getProductImage())
                .sellingPrice(item.getProductPrice())
                .build();

        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .product(productDTO)
                .build();
    }
}
