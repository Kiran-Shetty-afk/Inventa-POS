package com.zosh.service.impl;

import com.zosh.client.UserOrgClient;
import com.zosh.modal.Product;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.ProductRepository;
import com.zosh.repository.StockOperationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryAuthorizationTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productReadRejectsAUserOutsideTheProductStore() {
        ProductRepository products = mock(ProductRepository.class);
        UserOrgClient userOrg = mock(UserOrgClient.class);
        Product product = Product.builder().id(7L).storeId(8L).build();
        when(products.findById(7L)).thenReturn(Optional.of(product));
        when(userOrg.canCurrentUserAccessStore(8L)).thenReturn(false);
        authenticate("ROLE_BRANCH_MANAGER");

        ProductServiceImpl service = new ProductServiceImpl(products, null, userOrg);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getProductById(7L));
    }

    @Test
    void inventoryWriteRejectsAUserOutsideTheBranchStore() {
        ProductRepository products = mock(ProductRepository.class);
        InventoryRepository inventories = mock(InventoryRepository.class);
        UserOrgClient userOrg = mock(UserOrgClient.class);
        Product product = Product.builder().id(7L).storeId(8L).build();
        when(products.findById(7L)).thenReturn(Optional.of(product));
        when(userOrg.canCurrentUserAccessBranch(30L, 8L, "INVENTORY_WRITE")).thenReturn(false);
        authenticate("ROLE_BRANCH_MANAGER");

        InventoryServiceImpl service = new InventoryServiceImpl(
                inventories, products, userOrg, mock(StockOperationRepository.class));
        InventoryDTO request = InventoryDTO.builder().branchId(30L).productId(7L).quantity(5).build();

        assertThrows(com.zosh.exception.AccessDeniedException.class,
                () -> service.createInventory(request));
        verify(inventories, never()).saveAndFlush(any());
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test@example.invalid", null, List.of(new SimpleGrantedAuthority(role))));
    }
}
