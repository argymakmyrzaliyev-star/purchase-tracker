package kz.narxoz.purchase.config;

import kz.narxoz.purchase.domain.DuplicatePurchase;
import kz.narxoz.purchase.domain.PurchaseId;
import kz.narxoz.purchase.handler.PurchaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

/** Optional classroom demonstration using real, separate service calls. */
@Configuration
@Profile("demo")
public class PurchaseDemo {
    @Bean
    CommandLineRunner demonstrateTransactions(PurchaseService service) {
        return args -> {
            String suffix = UUID.randomUUID().toString();
            String rollbackKey = "PR-ROLLBACK-" + suffix;
            System.out.println("DEMO 1: two inserts in one transaction; key=" + rollbackKey);
            expectDuplicate(() -> service.insertTwice(rollbackKey));
            long rollbackCount = service.count(rollbackKey);
            System.out.println("ROLLBACK count = " + rollbackCount);
            require(rollbackCount == 0, "The failed transaction must leave no rows");

            String committedKey = "PR-COMMITTED-" + suffix;
            System.out.println("DEMO 2: two separate register calls; key=" + committedKey);
            var first = service.register(PurchaseId.newId(), committedKey, "Laptop for design team");
            expectDuplicate(() -> service.register(PurchaseId.newId(), committedKey, "Second request"));
            long committedCount = service.count(committedKey);
            System.out.println("COMMITTED count = " + committedCount);
            require(committedCount == 1, "The first committed request must remain");
            require(service.find(committedKey).orElseThrow().equals(first), "The original row changed");
            System.out.println("Original purchase retained: " + first);
            System.out.println("LAB 3 DEMO PASSED");
        };
    }

    private static void expectDuplicate(Runnable operation) {
        try {
            operation.run();
        } catch (DuplicatePurchase ex) {
            System.out.println("Expected domain error: " + ex.getMessage());
            return;
        }
        throw new IllegalStateException("Expected DuplicatePurchase was not thrown");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
