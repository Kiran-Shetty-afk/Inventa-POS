package com.zosh.repository;

import com.zosh.modal.Category;
import com.zosh.modal.Inventory;
import com.zosh.modal.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class InventoryRepositoryTest {

    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long branchId;
    private Long productId;

    @BeforeEach
    void setUp() {
        branchId = 101L;
        Category category = categoryRepository.save(Category.builder()
                .name("Test category")
                .storeId(55L)
                .build());
        Product product = productRepository.save(Product.builder()
                .name("Test product")
                .sku("TEST-" + UUID.randomUUID())
                .mrp(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("9.00"))
                .storeId(55L)
                .category(category)
                .build());
        productId = product.getId();
        inventoryRepository.saveAndFlush(Inventory.builder()
                .branchId(branchId)
                .product(product)
                .quantity(5)
                .build());
    }

    @Test
    void conditionalDecrementNeverAllowsStockBelowZero() {
        assertEquals(1, inventoryRepository.decrementStockIfAvailable(branchId, productId, 4));
        assertEquals(0, inventoryRepository.decrementStockIfAvailable(branchId, productId, 2));
        assertEquals(1, inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId)
                .get(0).getQuantity());
    }

    @Test
    void decrementWithNoInventoryRowDoesNotCreateOrChangeStock() {
        assertEquals(0, inventoryRepository.decrementStockIfAvailable(branchId + 1, productId, 1));
        assertEquals(5, inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId)
                .get(0).getQuantity());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentConditionalDecrementsCannotOversell() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var decrement = (java.util.concurrent.Callable<Integer>) () -> {
                ready.countDown();
                start.await();
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                return transaction.execute(status ->
                        inventoryRepository.decrementStockIfAvailable(branchId, productId, 4));
            };
            Future<Integer> first = workers.submit(decrement);
            Future<Integer> second = workers.submit(decrement);
            ready.await();
            start.countDown();

            assertEquals(1, first.get() + second.get());
            assertEquals(1, inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branchId, productId)
                    .get(0).getQuantity());
        } finally {
            workers.shutdownNow();
        }
    }
}
